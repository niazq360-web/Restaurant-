sed -i '/composable(Screen.Invoices.route) {/i \
        composable(Screen.SalesSummary.route) {\
            SalesSummaryScreen(viewModel = viewModel, onBack = { navController.popBackStack() })\
        }\
' app/src/main/java/com/example/MainActivity.kt
