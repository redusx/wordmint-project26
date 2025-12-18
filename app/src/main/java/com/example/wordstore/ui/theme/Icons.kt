package com.example.wordstore.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Centralized icon set for WordStore app. This provides consistent icon usage throughout the app.
 * All icons are Material Icons - no emojis. Using only icons available in the base
 * material-icons-core package.
 */
object WordStoreIcons {
    // Navigation
    val Back: ImageVector = Icons.AutoMirrored.Filled.ArrowBack
    val BackOutlined: ImageVector = Icons.AutoMirrored.Outlined.ArrowBack
    val Close: ImageVector = Icons.Filled.Close
    val Menu: ImageVector = Icons.Filled.Menu
    val MoreVert: ImageVector = Icons.Filled.MoreVert
    val ChevronRight: ImageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight
    val ExpandMore: ImageVector = Icons.Filled.KeyboardArrowDown
    val ExpandLess: ImageVector = Icons.Filled.KeyboardArrowUp

    // Actions
    val Add: ImageVector = Icons.Filled.Add
    val Delete: ImageVector = Icons.Filled.Delete
    val Edit: ImageVector = Icons.Filled.Edit
    val Save: ImageVector = Icons.Filled.Check
    val Search: ImageVector = Icons.Filled.Search
    val Share: ImageVector = Icons.Filled.Share
    val Copy: ImageVector = Icons.Filled.Create
    val Refresh: ImageVector = Icons.Filled.Refresh
    val Download: ImageVector = Icons.Filled.KeyboardArrowDown
    val Clear: ImageVector = Icons.Filled.Clear

    // Content & Media - Using available alternatives
    val Book: ImageVector = Icons.Filled.Create
    val Article: ImageVector = Icons.Filled.Create
    val Description: ImageVector = Icons.Filled.Create
    val Language: ImageVector = Icons.Filled.Star // Fallback
    val Translate: ImageVector = Icons.Filled.Create // Fallback
    val VolumeUp: ImageVector = Icons.Filled.PlayArrow // Fallback
    val VolumeOff: ImageVector = Icons.Filled.Close
    val PlayArrow: ImageVector = Icons.Filled.PlayArrow

    // Word & Learning related
    val School: ImageVector = Icons.Filled.Create
    val AutoStories: ImageVector = Icons.Filled.Create
    val Lightbulb: ImageVector = Icons.Filled.Star
    val TipsAndUpdates: ImageVector = Icons.Filled.Star
    val StarOutlined: ImageVector = Icons.Outlined.Star
    val StarFilled: ImageVector = Icons.Filled.Star
    val Bookmark: ImageVector = Icons.Filled.Star
    val BookmarkBorder: ImageVector = Icons.Outlined.Star
    val Label: ImageVector = Icons.Filled.Create

    // Status & Feedback
    val Info: ImageVector = Icons.Filled.Info
    val InfoOutlined: ImageVector = Icons.Outlined.Info
    val Warning: ImageVector = Icons.Filled.Warning
    val Error: ImageVector = Icons.Filled.Warning
    val CheckCircle: ImageVector = Icons.Filled.CheckCircle
    val CheckCircleOutlined: ImageVector = Icons.Outlined.CheckCircle
    val Help: ImageVector = Icons.Filled.Info
    val HelpOutlined: ImageVector = Icons.Outlined.Info

    // Settings & Preferences
    val Settings: ImageVector = Icons.Filled.Settings
    val SettingsOutlined: ImageVector = Icons.Outlined.Settings
    val DarkMode: ImageVector = Icons.Filled.Settings
    val LightMode: ImageVector = Icons.Filled.Settings
    val Notifications: ImageVector = Icons.Filled.Notifications
    val NotificationsOutlined: ImageVector = Icons.Outlined.Notifications

    // Categories & Organization
    val Folder: ImageVector = Icons.Filled.Create
    val FolderOpen: ImageVector = Icons.Filled.Create

    // Connection & Cloud
    val Wifi: ImageVector = Icons.Filled.Check

    // User & Profile
    val Person: ImageVector = Icons.Filled.Person
    val PersonOutlined: ImageVector = Icons.Outlined.Person
    val AccountCircle: ImageVector = Icons.Filled.Person

    // Misc UI
    val Home: ImageVector = Icons.Filled.Home
    val HomeOutlined: ImageVector = Icons.Outlined.Home
    val Favorite: ImageVector = Icons.Filled.Favorite
    val FavoriteOutlined: ImageVector = Icons.Outlined.FavoriteBorder
    val Lock: ImageVector = Icons.Filled.Check
    val ArrowDropDown: ImageVector = Icons.Filled.ArrowDropDown
    val KeyboardArrowDown: ImageVector = Icons.Filled.KeyboardArrowDown
    val KeyboardArrowUp: ImageVector = Icons.Filled.KeyboardArrowUp
    val SwapVert: ImageVector = Icons.Filled.Refresh // Fallback

    // Text formatting
    val FormatQuote: ImageVector = Icons.Filled.Create
}
