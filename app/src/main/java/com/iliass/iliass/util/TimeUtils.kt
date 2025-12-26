package com.iliass.iliass.util

import java.util.concurrent.TimeUnit

object TimeUtils {

    /**
     * Convert timestamp to relative time string (e.g., "3 minutes ago", "2 months ago")
     */
    fun getRelativeTimeString(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        if (diff < 0) {
            return "Just now"
        }

        val seconds = TimeUnit.MILLISECONDS.toSeconds(diff)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
        val hours = TimeUnit.MILLISECONDS.toHours(diff)
        val days = TimeUnit.MILLISECONDS.toDays(diff)

        return when {
            seconds < 60 -> "Just now"
            minutes < 2 -> "1 minute ago"
            minutes < 60 -> "$minutes minutes ago"
            hours < 2 -> "1 hour ago"
            hours < 24 -> "$hours hours ago"
            days < 2 -> "Yesterday"
            days < 7 -> "$days days ago"
            days < 14 -> "1 week ago"
            days < 30 -> "${days / 7} weeks ago"
            days < 60 -> "1 month ago"
            days < 365 -> "${days / 30} months ago"
            days < 730 -> "1 year ago"
            else -> "${days / 365} years ago"
        }
    }

    /**
     * Get warranty status based on registration date
     * @param registrationDate The phone registration timestamp
     * @param warrantyMonths Number of months for warranty (default 12)
     * @return Pair of (isUnderWarranty, remainingDays or expiredDays)
     */
    fun getWarrantyStatus(registrationDate: Long, warrantyMonths: Int = 12): WarrantyStatus {
        val now = System.currentTimeMillis()
        val warrantyEndDate = registrationDate + TimeUnit.DAYS.toMillis((warrantyMonths * 30).toLong())
        val diff = warrantyEndDate - now
        val daysRemaining = TimeUnit.MILLISECONDS.toDays(diff)

        return if (diff > 0) {
            // Under warranty
            val monthsRemaining = daysRemaining / 30
            val daysRemainingAfterMonths = daysRemaining % 30

            val message = when {
                daysRemaining == 0L -> "Expires today"
                daysRemaining == 1L -> "1 day left"
                daysRemaining < 7 -> "$daysRemaining days left"
                daysRemaining < 30 -> "${daysRemaining / 7} weeks left"
                monthsRemaining < 1 -> "$daysRemainingAfterMonths days left"
                monthsRemaining == 1L && daysRemainingAfterMonths > 0 -> "1 month, $daysRemainingAfterMonths days left"
                monthsRemaining == 1L -> "1 month left"
                else -> "$monthsRemaining months left"
            }

            WarrantyStatus(true, daysRemaining, message)
        } else {
            // Warranty expired
            val daysExpired = -daysRemaining
            val monthsExpired = daysExpired / 30

            val message = when {
                daysExpired == 0L -> "Expired today"
                daysExpired == 1L -> "Expired 1 day ago"
                daysExpired < 7 -> "Expired $daysExpired days ago"
                daysExpired < 30 -> "Expired ${daysExpired / 7} weeks ago"
                monthsExpired < 1 -> "Expired $daysExpired days ago"
                monthsExpired == 1L -> "Expired 1 month ago"
                monthsExpired < 12 -> "Expired $monthsExpired months ago"
                else -> "Expired ${monthsExpired / 12} years ago"
            }

            WarrantyStatus(false, daysExpired, message)
        }
    }
}

data class WarrantyStatus(
    val isActive: Boolean,
    val daysValue: Long,
    val message: String
)