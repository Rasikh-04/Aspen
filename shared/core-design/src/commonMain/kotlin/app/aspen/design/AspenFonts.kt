package app.aspen.design

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import app.aspen.design.generated.resources.Res
import app.aspen.design.generated.resources.fraunces_regular
import app.aspen.design.generated.resources.noto_nastaliq_urdu_regular
import app.aspen.design.generated.resources.plus_jakarta_sans_regular
import org.jetbrains.compose.resources.Font

/**
 * Bundled OFL font families (docs/06 §2, docs/12 §3). Display = warm humanist serif (Fraunces);
 * body = soft humanist sans (Plus Jakarta Sans); [nastaliq] = Noto Nastaliq Urdu for Urdu, which
 * the platform default does not render well (docs/12 §3).
 */
@Immutable
data class AspenFonts(
    val display: FontFamily,
    val body: FontFamily,
    val nastaliq: FontFamily,
)

@Composable
internal fun aspenFonts(): AspenFonts = AspenFonts(
    display = FontFamily(Font(Res.font.fraunces_regular, FontWeight.Normal)),
    body = FontFamily(Font(Res.font.plus_jakarta_sans_regular, FontWeight.Normal)),
    nastaliq = FontFamily(Font(Res.font.noto_nastaliq_urdu_regular, FontWeight.Normal)),
)
