sed -i '153,160c\
    fun addSampleProducts() {\
        viewModelScope.launch {\
            val currentProducts = repository.dao.getAllProductsSync()\
            if (currentProducts.isEmpty()) {\
                val sampleItems = listOf(\
                    Product(name = "Plain Bread", price = 120.0, category = "Bakery"),\
                    Product(name = "Bran Bread", price = 150.0, category = "Bakery"),\
                    Product(name = "Croissant", price = 250.0, category = "Bakery"),\
                    Product(name = "Chocolate Croissant", price = 300.0, category = "Bakery"),\
                    Product(name = "Chocolate Cake (1 Lb)", price = 1200.0, category = "Bakery"),\
                    Product(name = "Black Forest Cake", price = 1100.0, category = "Bakery"),\
                    Product(name = "Pineapple Cake", price = 1000.0, category = "Bakery"),\
                    Product(name = "Vanilla Cupcake", price = 150.0, category = "Bakery"),\
                    Product(name = "Chocolate Cupcake", price = 180.0, category = "Bakery"),\
                    Product(name = "Fudge Brownie", price = 200.0, category = "Bakery"),\
                    Product(name = "Walnut Brownie", price = 250.0, category = "Bakery"),\
                    Product(name = "Apple Pie", price = 350.0, category = "Bakery"),\
                    Product(name = "Cheese Danish", price = 280.0, category = "Bakery"),\
                    Product(name = "Plain Bagel", price = 100.0, category = "Bakery"),\
                    Product(name = "Sesame Bagel", price = 120.0, category = "Bakery"),\
                    Product(name = "Glazed Donut", price = 150.0, category = "Bakery"),\
                    Product(name = "Chocolate Donut", price = 180.0, category = "Bakery"),\
                    Product(name = "Fruit Tart", price = 300.0, category = "Bakery"),\
                    Product(name = "Lemon Tart", price = 250.0, category = "Bakery"),\
                    Product(name = "Macarons (Box of 4)", price = 600.0, category = "Bakery"),\
                    Product(name = "Cinnamon Roll", price = 220.0, category = "Bakery"),\
                    Product(name = "Blueberry Muffin", price = 180.0, category = "Bakery"),\
                    Product(name = "Chocolate Chip Muffin", price = 200.0, category = "Bakery"),\
                    Product(name = "Chocolate Eclair", price = 250.0, category = "Bakery"),\
                    Product(name = "Swiss Roll", price = 200.0, category = "Bakery"),\
                    Product(name = "Chicken Patties", price = 100.0, category = "Bakery"),\
                    Product(name = "Vegetable Patties", price = 80.0, category = "Bakery"),\
                    Product(name = "Tea Rusk (Pack)", price = 150.0, category = "Bakery"),\
                    Product(name = "Almond Biscuits", price = 400.0, category = "Bakery"),\
                    Product(name = "Zeera Biscuits", price = 300.0, category = "Bakery"),\
                    Product(name = "Nan Khatai", price = 350.0, category = "Bakery"),\
                    Product(name = "Cake Rusk", price = 250.0, category = "Bakery"),\
                    Product(name = "Zinger Burger", price = 450.0, category = "Fast Food"),\
                    Product(name = "Mighty Zinger", price = 650.0, category = "Fast Food"),\
                    Product(name = "Beef Burger", price = 500.0, category = "Fast Food"),\
                    Product(name = "Double Beef Burger", price = 700.0, category = "Fast Food"),\
                    Product(name = "Chicken Wrap", price = 350.0, category = "Fast Food"),\
                    Product(name = "Club Sandwich", price = 400.0, category = "Fast Food"),\
                    Product(name = "BBQ Sandwich", price = 450.0, category = "Fast Food"),\
                    Product(name = "French Fries (Small)", price = 150.0, category = "Fast Food"),\
                    Product(name = "French Fries (Large)", price = 250.0, category = "Fast Food"),\
                    Product(name = "Loaded Fries", price = 450.0, category = "Fast Food"),\
                    Product(name = "Chicken Tikka Pizza (S)", price = 500.0, category = "Fast Food"),\
                    Product(name = "Chicken Tikka Pizza (M)", price = 900.0, category = "Fast Food"),\
                    Product(name = "Chicken Tikka Pizza (L)", price = 1300.0, category = "Fast Food"),\
                    Product(name = "Fajita Pizza (S)", price = 500.0, category = "Fast Food"),\
                    Product(name = "Fajita Pizza (M)", price = 900.0, category = "Fast Food"),\
                    Product(name = "Fajita Pizza (L)", price = 1300.0, category = "Fast Food"),\
                    Product(name = "Pepperoni Pizza (M)", price = 1000.0, category = "Fast Food"),\
                    Product(name = "Chicken Nuggets (6 pcs)", price = 300.0, category = "Fast Food"),\
                    Product(name = "Chicken Nuggets (10 pcs)", price = 450.0, category = "Fast Food"),\
                    Product(name = "Hot Wings (6 pcs)", price = 350.0, category = "Fast Food"),\
                    Product(name = "Fried Chicken (1 pc)", price = 200.0, category = "Fast Food"),\
                    Product(name = "Fried Chicken (3 pcs)", price = 550.0, category = "Fast Food"),\
                    Product(name = "Onion Rings", price = 200.0, category = "Fast Food"),\
                    Product(name = "Hot Dog", price = 250.0, category = "Fast Food"),\
                    Product(name = "Shawarma", price = 200.0, category = "Fast Food"),\
                    Product(name = "Chicken Karahi (Half)", price = 1200.0, category = "Main Course"),\
                    Product(name = "Chicken Karahi (Full)", price = 2200.0, category = "Main Course"),\
                    Product(name = "Mutton Karahi (Half)", price = 1800.0, category = "Main Course"),\
                    Product(name = "Mutton Karahi (Full)", price = 3400.0, category = "Main Course"),\
                    Product(name = "Chicken Biryani", price = 350.0, category = "Main Course"),\
                    Product(name = "Beef Biryani", price = 400.0, category = "Main Course"),\
                    Product(name = "Mutton Pulao", price = 500.0, category = "Main Course"),\
                    Product(name = "Daal Mash", price = 300.0, category = "Main Course"),\
                    Product(name = "Daal Chana", price = 250.0, category = "Main Course"),\
                    Product(name = "Mixed Sabzi", price = 250.0, category = "Main Course"),\
                    Product(name = "Palak Paneer", price = 450.0, category = "Main Course"),\
                    Product(name = "Chicken Tikka (Chest)", price = 400.0, category = "Main Course"),\
                    Product(name = "Chicken Tikka (Leg)", price = 350.0, category = "Main Course"),\
                    Product(name = "Seekh Kebab (Beef)", price = 450.0, category = "Main Course"),\
                    Product(name = "Chicken Malai Boti", price = 550.0, category = "Main Course"),\
                    Product(name = "Beef Nihari", price = 600.0, category = "Main Course"),\
                    Product(name = "Chicken Haleem", price = 350.0, category = "Main Course"),\
                    Product(name = "Chicken Handi", price = 1500.0, category = "Main Course"),\
                    Product(name = "Mutton Handi", price = 2400.0, category = "Main Course"),\
                    Product(name = "Brain Masala", price = 800.0, category = "Main Course"),\
                    Product(name = "Sada Roti", price = 20.0, category = "Sides"),\
                    Product(name = "Khamiri Roti", price = 30.0, category = "Sides"),\
                    Product(name = "Naan", price = 40.0, category = "Sides"),\
                    Product(name = "Garlic Naan", price = 60.0, category = "Sides"),\
                    Product(name = "Roghni Naan", price = 70.0, category = "Sides"),\
                    Product(name = "Cheese Naan", price = 150.0, category = "Sides"),\
                    Product(name = "Lachha Paratha", price = 80.0, category = "Sides"),\
                    Product(name = "Fresh Salad", price = 100.0, category = "Sides"),\
                    Product(name = "Zeera Raita", price = 80.0, category = "Sides"),\
                    Product(name = "Mint Raita", price = 80.0, category = "Sides"),\
                    Product(name = "Kachumar Salad", price = 120.0, category = "Sides"),\
                    Product(name = "Russian Salad", price = 250.0, category = "Sides"),\
                    Product(name = "Mineral Water (Small)", price = 60.0, category = "Beverages"),\
                    Product(name = "Mineral Water (Large)", price = 100.0, category = "Beverages"),\
                    Product(name = "Soft Drink (Can)", price = 120.0, category = "Beverages"),\
                    Product(name = "Soft Drink (1.5 Ltr)", price = 220.0, category = "Beverages"),\
                    Product(name = "Fresh Lime", price = 150.0, category = "Beverages"),\
                    Product(name = "Mint Margarita", price = 250.0, category = "Beverages"),\
                    Product(name = "Pina Colada", price = 350.0, category = "Beverages"),\
                    Product(name = "Cold Coffee", price = 300.0, category = "Beverages"),\
                    Product(name = "Karak Chai", price = 80.0, category = "Beverages"),\
                    Product(name = "Green Tea", price = 70.0, category = "Beverages"),\
                    Product(name = "Gulab Jamun (2 pcs)", price = 150.0, category = "Desserts"),\
                    Product(name = "Rasmalai (2 pcs)", price = 200.0, category = "Desserts"),\
                    Product(name = "Kheer", price = 200.0, category = "Desserts"),\
                    Product(name = "Ice Cream (1 Scoop)", price = 150.0, category = "Desserts"),\
                    Product(name = "Gajar Halwa", price = 300.0, category = "Desserts")\
                )\
                sampleItems.forEach { product ->\
                    repository.insertProduct(product)\
                }\
            }\
        }\
    }\
' app/src/main/java/com/example/ui/pos/PosViewModel.kt
