package manoellribeiro.dev.martp.core.utils

import manoellribeiro.dev.martp.scenes.artStyleSettings.ColorPalette

object ColorPaletteProvider {

    val all: List<ColorPalette> = listOf(
        ColorPalette(
            name = "Martp",
            colors = listOf(
                0xFF100C2D.toInt(),
                0xFF120D31.toInt(),
                0xFF4E3C41.toInt(),
                0xFF896B51.toInt(),
                0xFFFFC971.toInt(),
                0xFFFFCE7E.toInt(),
            )
        ),
        ColorPalette(
            name = "Benedictus",
            colors = listOf(
                0xFFF27EA9.toInt(),
                0xFF366CD9.toInt(),
                0xFF9475C1.toInt(),
                0xFF5EADF2.toInt(),
                0xFF636E73.toInt(),
                0xFFF2E6D8.toInt(),
            )
        ),
        ColorPalette(
            name = "Cross",
            colors = listOf(
                0xFFD962AF.toInt(),
                0xFF58A6A6.toInt(),
                0xFF8AA66F.toInt(),
                0xFFF29F05.toInt(),
                0xFFF26D6D.toInt(),
                0xFFF28639.toInt(),
            )
        ),
        ColorPalette(
            name = "Demuth",
            colors = listOf(
                0xFF222940.toInt(),
                0xFFD98E04.toInt(),
                0xFFF2A950.toInt(),
                0xFFBF3E21.toInt(),
                0xFFF2F2F2.toInt(),
                0xFFD97439.toInt(),
            )
        ),
        ColorPalette(
            name = "Hiroshige",
            colors = listOf(
                0xFF1B618C.toInt(),
                0xFF55CCD9.toInt(),
                0xFFF2BC57.toInt(),
                0xFFF2DAAC.toInt(),
                0xFFF24949.toInt(),
                0xFFA4C498.toInt(),
            )
        ),
        ColorPalette(
            name = "Hokusai",
            colors = listOf(
                0xFF074A59.toInt(),
                0xFFF2C166.toInt(),
                0xFFF28241.toInt(),
                0xFFF26B5E.toInt(),
                0xFFF2F2F2.toInt(),
                0xFF7D8660.toInt(),
            )
        ),
        ColorPalette(
            name = "Hokusai Blue",
            colors = listOf(
                0xFF023059.toInt(),
                0xFF459DBF.toInt(),
                0xFF87BF60.toInt(),
                0xFFD9D16A.toInt(),
                0xFFF2F2F2.toInt(),
                0xFF66AE90.toInt(),
            )
        ),
        ColorPalette(
            name = "Java",
            colors = listOf(
                0xFF632973.toInt(),
                0xFF02734A.toInt(),
                0xFFF25C05.toInt(),
                0xFFF29188.toInt(),
                0xFFF2E0DF.toInt(),
                0xFF7A6828.toInt(),
            )
        ),
        ColorPalette(
            name = "Kandinsky",
            colors = listOf(
                0xFF8D95A6.toInt(),
                0xFF0A7360.toInt(),
                0xFFF28705.toInt(),
                0xFFD98825.toInt(),
                0xFFF2F2F2.toInt(),
                0xFFE6BD8C.toInt(),
            )
        ),
        ColorPalette(
            name = "Monet",
            colors = listOf(
                0xFF4146A6.toInt(),
                0xFF063573.toInt(),
                0xFF5EC8F2.toInt(),
                0xFF8C4E03.toInt(),
                0xFFD98A29.toInt(),
                0xFF758B7B.toInt(),
            )
        ),
        ColorPalette(
            name = "Nizami",
            colors = listOf(
                0xFF034AA6.toInt(),
                0xFF72B6F2.toInt(),
                0xFF73BFB1.toInt(),
                0xFFF2A30F.toInt(),
                0xFFF26F63.toInt(),
                0xFFF28939.toInt(),
            )
        ),
        ColorPalette(
            name = "Renoir",
            colors = listOf(
                0xFF303E8C.toInt(),
                0xFFF2AE2E.toInt(),
                0xFFF28705.toInt(),
                0xFFD91414.toInt(),
                0xFFF2F2F2.toInt(),
                0xFF91765D.toInt(),
            )
        ),
        ColorPalette(
            name = "VanGogh",
            colors = listOf(
                0xFF424D8C.toInt(),
                0xFF84A9BF.toInt(),
                0xFFC1D9CE.toInt(),
                0xFFF2B705.toInt(),
                0xFFF25C05.toInt(),
                0xFFDAC86A.toInt(),
            )
        ),
        ColorPalette(
            name = "Mono",
            colors = listOf(
                0xFFD9D7D8.toInt(),
                0xFF3B5159.toInt(),
                0xFF5D848C.toInt(),
                0xFF7CA2A6.toInt(),
                0xFF262321.toInt(),
                0xFF3C4343.toInt(),
            )
        ),
        ColorPalette(
            name = "RiverSide",
            colors = listOf(
                0xFF906FA6.toInt(),
                0xFF025951.toInt(),
                0xFF252625.toInt(),
                0xFFD99191.toInt(),
                0xFFF2F2F2.toInt(),
                0xFF14403B.toInt(),
            )
        ),
    )

    fun byNameOrDefault(name: String?): ColorPalette = all.firstOrNull { colorPalette -> colorPalette.name == name } ?: all.first()
}