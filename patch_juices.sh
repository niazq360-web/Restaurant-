sed -i '/fun addSampleProducts() {/i \
    fun addJuiceProducts() {\
        viewModelScope.launch {\
            val currentProducts = repository.dao.getAllProductsSync()\
            if (currentProducts.none { it.category == "Juices" }) {\
                val juiceItems = listOf(\
                    Product(name = "Apple Juice", price = 200.0, category = "Juices"),\
                    Product(name = "Orange Juice", price = 250.0, category = "Juices"),\
                    Product(name = "Mango Juice", price = 250.0, category = "Juices"),\
                    Product(name = "Pineapple Juice", price = 300.0, category = "Juices"),\
                    Product(name = "Watermelon Juice", price = 150.0, category = "Juices"),\
                    Product(name = "Strawberry Juice", price = 300.0, category = "Juices"),\
                    Product(name = "Pomegranate Juice", price = 350.0, category = "Juices"),\
                    Product(name = "Grape Juice", price = 250.0, category = "Juices"),\
                    Product(name = "Peach Juice", price = 200.0, category = "Juices"),\
                    Product(name = "Mixed Fruit Juice", price = 350.0, category = "Juices")\
                )\
                juiceItems.forEach {\
                    repository.insertProduct(it)\
                }\
            }\
        }\
    }\
' app/src/main/java/com/example/ui/pos/PosViewModel.kt
