package com.landpoint.app.ui.owners

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.landpoint.app.AppContainer
import com.landpoint.app.R
import com.landpoint.app.data.PhotoStore
import com.landpoint.app.data.model.Owner
import com.landpoint.app.ui.container
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OwnerFormUiState(
    val id: String? = null,
    val fullName: String = "",
    val nik: String = "",
    val address: String = "",
    val phone: String = "",
    val email: String = "",
    val photoPath: String? = null,
    @StringRes val nameError: Int? = null,
    @StringRes val nikError: Int? = null,
    @StringRes val phoneError: Int? = null,
    @StringRes val emailError: Int? = null,
    @StringRes val duplicateNikWarning: Int? = null,
    val isSaving: Boolean = false,
    val createdAt: Long? = null
) {
    val isEdit: Boolean get() = id != null
}

/**
 * Field validation for the owner form, in the open so it can be unit tested
 * without a device: every message is a sentence a person can act on, not a
 * technical complaint.
 */
object OwnerValidation {

    private val NIK_DIGITS = Regex("^\\d{16}$")
    private val PHONE_CHARS = Regex("^[+0-9 ()\\-]{5,20}$")
    private val EMAIL = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

    @StringRes
    fun fullNameError(value: String): Int? = if (value.isBlank()) R.string.error_owner_name_required else null

    @StringRes
    fun nikError(value: String): Int? = when {
        value.isBlank() -> null
        !value.all { it.isDigit() } -> R.string.error_owner_nik_digits
        value.length != 16 -> R.string.error_owner_nik_length
        else -> null
    }

    @StringRes
    fun phoneError(value: String): Int? = when {
        value.isBlank() -> null
        !PHONE_CHARS.matches(value) -> R.string.error_owner_phone
        else -> null
    }

    @StringRes
    fun emailError(value: String): Int? = when {
        value.isBlank() -> null
        !EMAIL.matches(value) -> R.string.error_owner_email
        else -> null
    }
}

class OwnerFormViewModel(private val container: AppContainer, savedStateHandle: SavedStateHandle) : ViewModel() {

    private val repository = container.repository
    private val photoStore: PhotoStore = container.photoStore

    private val ownerId: String? = savedStateHandle["ownerId"]

    private val _uiState = MutableStateFlow(OwnerFormUiState())
    val uiState: StateFlow<OwnerFormUiState> = _uiState.asStateFlow()

    init {
        ownerId?.let { id ->
            viewModelScope.launch {
                repository.getOwner(id)?.let { owner ->
                    _uiState.update {
                        it.copy(
                            id = owner.id,
                            fullName = owner.fullName,
                            nik = owner.nik ?: "",
                            address = owner.address,
                            phone = owner.phone ?: "",
                            email = owner.email ?: "",
                            photoPath = owner.photoPath,
                            createdAt = owner.createdAt
                        )
                    }
                }
            }
        }
    }

    fun setFullName(value: String) = _uiState.update { it.copy(fullName = value, nameError = null) }
    fun setNik(value: String) = _uiState.update { it.copy(nik = value, nikError = null, duplicateNikWarning = null) }
    fun setAddress(value: String) = _uiState.update { it.copy(address = value) }
    fun setPhone(value: String) = _uiState.update { it.copy(phone = value, phoneError = null) }
    fun setEmail(value: String) = _uiState.update { it.copy(email = value, emailError = null) }

    /** Called by the camera/gallery pickers with the stored file path. */
    fun setPhotoPath(path: String?) = _uiState.update { it.copy(photoPath = path) }

    fun newCameraTarget() = photoStore.newCameraTarget()

    /** Persists a finished camera capture; safe to call from any callback. */
    fun requestPersistCapture(file: java.io.File) {
        viewModelScope.launch { setPhotoPath(photoStore.persistCapture(file)) }
    }

    /** Copies a user-picked image; safe to call from any callback. */
    fun requestImportFromUri(uri: android.net.Uri) {
        viewModelScope.launch { setPhotoPath(photoStore.importFromUri(uri)) }
    }

    fun save(onSaved: () -> Unit) {
        val state = _uiState.value
        val nameError = OwnerValidation.fullNameError(state.fullName.trim())
        val nikError = OwnerValidation.nikError(state.nik.trim())
        val phoneError = OwnerValidation.phoneError(state.phone.trim())
        val emailError = OwnerValidation.emailError(state.email.trim())

        if (nameError != null || nikError != null || phoneError != null || emailError != null) {
            _uiState.update {
                it.copy(
                    nameError = nameError,
                    nikError = nikError,
                    phoneError = phoneError,
                    emailError = emailError
                )
            }
            return
        }

        _uiState.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val trimmedNik = state.nik.trim().ifBlank { null }
            // A second owner with the same NIK is almost always a typo; it is
            // allowed (families do share forms), but the warning says so first.
            val duplicate = trimmedNik?.let { repository.findOwnerByNik(it) }
            if (duplicate != null && duplicate.id != state.id) {
                _uiState.update { it.copy(isSaving = false, duplicateNikWarning = R.string.warning_owner_nik_duplicate) }
                return@launch
            }
            val owner = Owner(
                id = state.id ?: java.util.UUID.randomUUID().toString(),
                fullName = state.fullName.trim(),
                nik = trimmedNik,
                address = state.address.trim(),
                phone = state.phone.trim().ifBlank { null },
                email = state.email.trim().ifBlank { null },
                photoPath = state.photoPath,
                createdAt = state.createdAt ?: now,
                updatedAt = now
            )
            if (state.isEdit) {
                repository.updateOwner(owner)
            } else {
                repository.saveOwner(owner)
            }
            _uiState.update { it.copy(isSaving = false) }
            onSaved()
        }
    }

    companion object {
        val FACTORY = viewModelFactory {
            initializer {
                OwnerFormViewModel(container(), createSavedStateHandle())
            }
        }
    }
}
