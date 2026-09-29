sed -i '/import coil.compose.AsyncImage/d' app/src/main/java/com/example/ui/pos/PosScreen.kt
sed -i '/import androidx.compose.ui.layout.ContentScale/d' app/src/main/java/com/example/ui/pos/PosScreen.kt
sed -i 's/import androidx.compose.material.icons.filled.Remove/import androidx.compose.material.icons.filled.Remove\nimport androidx.compose.material.icons.filled.Edit/g' app/src/main/java/com/example/ui/pos/PosScreen.kt

