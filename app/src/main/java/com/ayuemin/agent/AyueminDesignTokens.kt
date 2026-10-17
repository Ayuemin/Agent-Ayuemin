package com.ayuemin.agent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

object AyueminDesignTokens {
 val panel=RoundedCornerShape(22.dp)
 val item=RoundedCornerShape(16.dp)
 val field=RoundedCornerShape(18.dp)
}
@Composable
fun AyueminPanel(modifier:Modifier=Modifier,content:@Composable ColumnScope.()->Unit){
 Surface(modifier=modifier,shape=AyueminDesignTokens.panel,
 color=MaterialTheme.colorScheme.surfaceContainerLow,
 border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)){
  Column(Modifier.padding(16.dp),content=content)
 }
}
