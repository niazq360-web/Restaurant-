sed -i 's/var productToEdit by remember { mutableStateOf<Product?>(null) }/var productToEdit by remember { mutableStateOf<Product?>(null) }\n    var searchQuery by remember { mutableStateOf("") }\n    val filteredProducts = products.filter { \n        it.name.contains(searchQuery, ignoreCase = true) || \n        it.category.contains(searchQuery, ignoreCase = true) \n    }/' app/src/main/java/com/example/ui/pos/PosScreen.kt

sed -i 's/Text("+ Add Custom Item")/Text("+ Add Custom Item")/' app/src/main/java/com/example/ui/pos/PosScreen.kt
# need to add search bar below the Row

sed -i '/Spacer(modifier = Modifier.height(16.dp))/a \
            OutlinedTextField(\n                value = searchQuery,\n                onValueChange = { searchQuery = it },\n                label = { Text("Search by name or category") },\n                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),\n                singleLine = true\n            )' app/src/main/java/com/example/ui/pos/PosScreen.kt

sed -i 's/items(products) { product ->/items(filteredProducts) { product ->/' app/src/main/java/com/example/ui/pos/PosScreen.kt
