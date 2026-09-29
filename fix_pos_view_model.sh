sed -i '190,205d' app/src/main/java/com/example/ui/pos/PosViewModel.kt
sed -i '/fun deleteProduct(product: Product) {/,$d' app/src/main/java/com/example/ui/pos/PosViewModel.kt

cat << 'INNER_EOF' >> app/src/main/java/com/example/ui/pos/PosViewModel.kt
    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            repository.deleteProduct(product)
        }
    }

    suspend fun exportData(context: android.content.Context): android.content.Intent? {
        return com.example.data.ExportImportHelper.exportDatabaseToJson(context, repository.dao)
    }

    fun importData(context: android.content.Context, uri: android.net.Uri) {
        viewModelScope.launch {
            com.example.data.ExportImportHelper.importDatabaseFromJson(context, uri, repository.dao)
        }
    }
}
INNER_EOF
