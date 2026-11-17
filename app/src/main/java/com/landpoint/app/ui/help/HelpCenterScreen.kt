package com.landpoint.app.ui.help

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.landpoint.app.R
import com.landpoint.app.ui.theme.Spacing
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The help centre: the bundled markdown page for the app's language, rendered
 * in-app (docs/IMPLEMENTATION_PLAN.md, Phase 4).
 *
 * No network anywhere. The page ships in the APK, so the manual works in the
 * same conditions as the features it explains — standing in a field, out of
 * signal, wondering how something works.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpCenterScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    // The language the app is showing right now — the wrapped context's
    // configuration already carries the in-app override, so the help page
    // follows the same choice as every string on screen.
    val languageTag = context.resources.configuration.locales[0].language
    val assetPath = remember(languageTag) { HelpMarkdown.assetFor(languageTag) }

    // Null while the asset is read; set once parsed; [failed] says the asset
    // could not be opened at all, which on a bundled page means a packaging
    // mistake — still worth naming rather than showing a blank screen.
    var blocks by remember(assetPath) { mutableStateOf<List<HelpBlock>?>(null) }
    var failed by remember(assetPath) { mutableStateOf(false) }

    LaunchedEffect(assetPath) {
        try {
            blocks = withContext(Dispatchers.IO) { HelpMarkdown.load(context, assetPath) }
        } catch (e: IOException) {
            failed = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.help_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        val loaded = blocks
        when {
            loaded != null -> HelpPage(
                blocks = loaded,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )

            failed -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(Spacing.lg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    stringResource(R.string.help_unavailable),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            else -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Text(
                        stringResource(R.string.help_loading),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = Spacing.md)
                    )
                }
            }
        }
    }
}

@Composable
private fun HelpPage(blocks: List<HelpBlock>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.lg, vertical = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        blocks.forEach { block -> HelpBlockView(block) }
    }
}

@Composable
private fun HelpBlockView(block: HelpBlock) {
    when (block) {
        is HelpBlock.Heading -> Text(
            block.text,
            style = when (block.level) {
                1 -> MaterialTheme.typography.headlineSmall
                2 -> MaterialTheme.typography.titleLarge
                else -> MaterialTheme.typography.titleMedium
            },
            modifier = Modifier.padding(top = Spacing.sm)
        )

        is HelpBlock.Paragraph -> Text(
            inline(block.segments),
            style = MaterialTheme.typography.bodyLarge
        )

        is HelpBlock.Bullet -> Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 9.dp)
                    .width(6.dp)
                    .height(6.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
            Text(
                inline(block.segments),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/** Turns the parsed runs into one string with the bold spans applied. */
private fun inline(segments: List<HelpSegment>): AnnotatedString = buildAnnotatedString {
    segments.forEach { segment ->
        if (segment.bold) {
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(segment.text) }
        } else {
            append(segment.text)
        }
    }
}
