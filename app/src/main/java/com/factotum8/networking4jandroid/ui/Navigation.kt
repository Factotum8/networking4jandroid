package com.factotum8.networking4jandroid.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.factotum8.networking4jandroid.ui.contactdetail.ContactDetailScreen
import com.factotum8.networking4jandroid.ui.contacts.ContactsScreen
import com.factotum8.networking4jandroid.ui.dashboard.DashboardScreen
import com.factotum8.networking4jandroid.ui.duplicates.DuplicatesScreen
import com.factotum8.networking4jandroid.ui.graph.GraphScreen
import com.factotum8.networking4jandroid.ui.importexport.ImportExportScreen
import com.factotum8.networking4jandroid.ui.more.MoreScreen
import com.factotum8.networking4jandroid.ui.settings.SettingsScreen

/**
 * The phone nav (decision #1): the source's 6-link top bar becomes a
 * bottom nav bar with 4 primary destinations, "Ещё" opening a menu for
 * the 3 secondary pages — 6 items in a bottom bar doesn't fit phone
 * width/UX guidance.
 */
private data class BottomNavItem(val route: String, val label: String, val icon: ImageVector)

private val BOTTOM_NAV_ITEMS = listOf(
    BottomNavItem(Routes.DASHBOARD, "Дашборд", Icons.Default.Home),
    BottomNavItem(Routes.CONTACTS, "Контакты", Icons.Default.Person),
    BottomNavItem(Routes.GRAPH, "Граф", Icons.Default.Share),
    BottomNavItem(Routes.MORE, "Ещё", Icons.Default.MoreVert),
)

@Composable
fun NetworkingApp() {
    val navController = rememberNavController()
    Scaffold(
        bottomBar = { NetworkingBottomBar(navController) },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.DASHBOARD,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.DASHBOARD) {
                DashboardScreen(onOpenContact = { navController.navigate(Routes.contactDetail(it)) })
            }
            composable(Routes.CONTACTS) {
                ContactsScreen(
                    onOpenContact = { navController.navigate(Routes.contactDetail(it)) },
                    onNewContact = { navController.navigate(Routes.contactDetail(Routes.NEW_CONTACT_ID)) },
                )
            }
            composable(
                route = Routes.CONTACT_DETAIL,
                arguments = listOf(navArgument(Routes.CONTACT_ID_ARG) { type = NavType.StringType }),
            ) { backStackEntry ->
                val contactId = backStackEntry.arguments?.getString(Routes.CONTACT_ID_ARG) ?: Routes.NEW_CONTACT_ID
                ContactDetailScreen(contactId = contactId, onBack = { navController.popBackStack() })
            }
            composable(Routes.GRAPH) {
                GraphScreen(onOpenContact = { navController.navigate(Routes.contactDetail(it)) })
            }
            composable(Routes.MORE) {
                MoreScreen(
                    onOpenDuplicates = { navController.navigate(Routes.DUPLICATES) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    onOpenImportExport = { navController.navigate(Routes.IMPORT_EXPORT) },
                )
            }
            composable(Routes.DUPLICATES) {
                DuplicatesScreen(
                    onBack = { navController.popBackStack() },
                    onOpenContact = { navController.navigate(Routes.contactDetail(it)) },
                )
            }
            composable(Routes.SETTINGS) { SettingsScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.IMPORT_EXPORT) { ImportExportScreen(onBack = { navController.popBackStack() }) }
        }
    }
}

@Composable
private fun NetworkingBottomBar(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    NavigationBar {
        BOTTOM_NAV_ITEMS.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
            )
        }
    }
}
