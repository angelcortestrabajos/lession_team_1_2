package com.example.lession_equipo1.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    val colors = LocalAppColors.current
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = colors.subText, fontSize = 12.sp) },
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = backgroundColor,
            unfocusedContainerColor = backgroundColor,
            disabledContainerColor = backgroundColor,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedTextColor = colors.text,
            unfocusedTextColor = colors.text
        ),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp, color = colors.text)
    )
}

@Composable
fun LessionLogoGraphic(modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF322285))
                .border(1.5.dp, Color(0xFFCCFF00), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(26.dp)) {
                val w = size.width
                val h = size.height
                val shieldPath = androidx.compose.ui.graphics.Path().apply {
                    moveTo(w * 0.3f, h * 0.2f)
                    lineTo(w * 0.5f, h * 0.1f)
                    lineTo(w * 0.7f, h * 0.2f)
                    lineTo(w * 0.7f, h * 0.6f)
                    quadraticBezierTo(w * 0.5f, h * 0.9f, w * 0.5f, h * 0.9f)
                    quadraticBezierTo(w * 0.5f, h * 0.9f, w * 0.3f, h * 0.6f)
                    close()
                }
                drawPath(shieldPath, color = Color(0xFFCCFF00), style = Stroke(width = 2.5f))
                
                // ECG Waveform inside shield
                val ecgPath = androidx.compose.ui.graphics.Path().apply {
                    moveTo(w * 0.35f, h * 0.55f)
                    lineTo(w * 0.45f, h * 0.55f)
                    lineTo(w * 0.5f, h * 0.35f)
                    lineTo(w * 0.56f, h * 0.7f)
                    lineTo(w * 0.62f, h * 0.45f)
                    lineTo(w * 0.68f, h * 0.55f)
                }
                drawPath(ecgPath, color = Color(0xFFCCFF00), style = Stroke(width = 1.8f))
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            "LESSiON",
            color = colors.text,
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            fontStyle = FontStyle.Italic,
            letterSpacing = 1.sp
        )
    }
}
