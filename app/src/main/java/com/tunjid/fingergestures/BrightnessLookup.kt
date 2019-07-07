/*
 * Copyright (c) 2017, 2018, 2019 Adetunji Dahunsi.
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.tunjid.fingergestures

object BrightnessLookup {

    private val FLICKER_THRESHOLD = 24

    private val table = arrayOf(intArrayOf(0, 0), intArrayOf(1, 6), intArrayOf(2, 11), intArrayOf(3, 15), intArrayOf(4, 19), intArrayOf(5, 22), intArrayOf(6, 24), intArrayOf(7, 27), intArrayOf(8, 29), intArrayOf(9, 31), intArrayOf(10, 33), intArrayOf(11, 34), intArrayOf(12, 36), intArrayOf(13, 38), intArrayOf(14, 39), intArrayOf(15, 41), intArrayOf(16, 42), intArrayOf(17, 43), intArrayOf(18, 45), intArrayOf(19, 46), intArrayOf(20, 47), intArrayOf(21, 49), intArrayOf(22, 50), intArrayOf(23, 51), intArrayOf(24, 52), intArrayOf(25, 53), intArrayOf(26, 54), intArrayOf(27, 55), intArrayOf(28, 56), intArrayOf(29, 57), intArrayOf(30, 57), intArrayOf(31, 57), intArrayOf(32, 57), intArrayOf(33, 60), intArrayOf(34, 60), intArrayOf(35, 61), intArrayOf(36, 62), intArrayOf(37, 62), intArrayOf(38, 63), intArrayOf(39, 63), intArrayOf(40, 64), intArrayOf(41, 64), intArrayOf(42, 65), intArrayOf(43, 65), intArrayOf(44, 66), intArrayOf(45, 66), intArrayOf(46, 67), intArrayOf(47, 67), intArrayOf(48, 68), intArrayOf(49, 68), intArrayOf(50, 69), intArrayOf(51, 69), intArrayOf(52, 70), intArrayOf(53, 70), intArrayOf(54, 70), intArrayOf(55, 71), intArrayOf(56, 71), intArrayOf(57, 71), intArrayOf(58, 72), intArrayOf(59, 72), intArrayOf(60, 72), intArrayOf(61, 73), intArrayOf(62, 73), intArrayOf(63, 73), intArrayOf(64, 74), intArrayOf(65, 74), intArrayOf(66, 74), intArrayOf(67, 75), intArrayOf(68, 75), intArrayOf(71, 76), intArrayOf(73, 76), intArrayOf(76, 77), intArrayOf(79, 78), intArrayOf(80, 78), intArrayOf(81, 78), intArrayOf(84, 79), intArrayOf(86, 80), intArrayOf(89, 80), intArrayOf(91, 81), intArrayOf(94, 81), intArrayOf(96, 82), intArrayOf(97, 82), intArrayOf(98, 82), intArrayOf(99, 82), intArrayOf(102, 83), intArrayOf(104, 83), intArrayOf(107, 84), intArrayOf(109, 84), intArrayOf(112, 85), intArrayOf(114, 85), intArrayOf(117, 85), intArrayOf(119, 86), intArrayOf(122, 87), intArrayOf(124, 87), intArrayOf(127, 87), intArrayOf(130, 88), intArrayOf(132, 88), intArrayOf(133, 88), intArrayOf(134, 88), intArrayOf(135, 88), intArrayOf(136, 88), intArrayOf(137, 88), intArrayOf(138, 89), intArrayOf(139, 89), intArrayOf(140, 89), intArrayOf(141, 89), intArrayOf(142, 89), intArrayOf(143, 89), intArrayOf(144, 89), intArrayOf(145, 90), intArrayOf(146, 90), intArrayOf(147, 90), intArrayOf(148, 90), intArrayOf(149, 90), intArrayOf(150, 90), intArrayOf(151, 90), intArrayOf(152, 91), intArrayOf(153, 91), intArrayOf(154, 91), intArrayOf(155, 91), intArrayOf(156, 91), intArrayOf(157, 91), intArrayOf(158, 91), intArrayOf(159, 91), intArrayOf(160, 91), intArrayOf(161, 91), intArrayOf(162, 92), intArrayOf(163, 92), intArrayOf(164, 92), intArrayOf(165, 92), intArrayOf(166, 92), intArrayOf(167, 92), intArrayOf(168, 92), intArrayOf(169, 92), intArrayOf(170, 92), intArrayOf(171, 93), intArrayOf(172, 93), intArrayOf(173, 93), intArrayOf(174, 93), intArrayOf(175, 93), intArrayOf(176, 93), intArrayOf(177, 93), intArrayOf(178, 93), intArrayOf(179, 93), intArrayOf(180, 94), intArrayOf(181, 94), intArrayOf(182, 94), intArrayOf(183, 94), intArrayOf(184, 94), intArrayOf(185, 94), intArrayOf(186, 94), intArrayOf(187, 94), intArrayOf(188, 94), intArrayOf(189, 94), intArrayOf(190, 95), intArrayOf(191, 95), intArrayOf(192, 95), intArrayOf(193, 95), intArrayOf(194, 95), intArrayOf(195, 95), intArrayOf(196, 95), intArrayOf(197, 95), intArrayOf(198, 95), intArrayOf(199, 95), intArrayOf(200, 96), intArrayOf(201, 96), intArrayOf(202, 96), intArrayOf(203, 96), intArrayOf(204, 96), intArrayOf(205, 96), intArrayOf(206, 96), intArrayOf(207, 96), intArrayOf(208, 96), intArrayOf(209, 96), intArrayOf(210, 96), intArrayOf(211, 96), intArrayOf(212, 97), intArrayOf(213, 97), intArrayOf(214, 97), intArrayOf(215, 97), intArrayOf(216, 97), intArrayOf(217, 97), intArrayOf(218, 97), intArrayOf(219, 97), intArrayOf(220, 97), intArrayOf(221, 97), intArrayOf(222, 97), intArrayOf(223, 98), intArrayOf(224, 98), intArrayOf(225, 98), intArrayOf(226, 98), intArrayOf(227, 98), intArrayOf(228, 98), intArrayOf(229, 98), intArrayOf(230, 98), intArrayOf(231, 98), intArrayOf(232, 98), intArrayOf(233, 98), intArrayOf(234, 98), intArrayOf(235, 99), intArrayOf(236, 99), intArrayOf(237, 99), intArrayOf(238, 99), intArrayOf(239, 99), intArrayOf(240, 99), intArrayOf(241, 99), intArrayOf(242, 99), intArrayOf(243, 99), intArrayOf(244, 99), intArrayOf(245, 99), intArrayOf(246, 99), intArrayOf(247, 99), intArrayOf(248, 100), intArrayOf(249, 100), intArrayOf(250, 100), intArrayOf(251, 100), intArrayOf(252, 100), intArrayOf(253, 100), intArrayOf(254, 100), intArrayOf(255, 100))

    fun lookup(query: Int, isByte: Boolean): Int {
        var low = 0
        var mid = 0
        var high = table.size - 1
        var increasing = true

        // No direct mapping for bytes, just crawl for the lower values
        if (!isByte && query < FLICKER_THRESHOLD) return crawl(query, 0, false, true)

        while (low <= high) {
            mid = (low + high) / 2
            val key = table[mid][if (isByte) 0 else 1]
            val diff = query - key
            increasing = diff > 0

            if (Math.abs(diff) < 2)
                return crawl(query, mid, isByte, increasing)
            else if (increasing)
                low = mid + 1
            else
                high = mid - 1
        }

        return crawl(query, mid, isByte, increasing)
    }

    private fun crawl(query: Int, index: Int, isByte: Boolean, increasing: Boolean): Int {
        var i = index
        val num = table.size
        val key = if (isByte) 0 else 1
        val value = if (isByte) 1 else 0

        if (increasing) {
            while (i < num) {
                if (table[i][key] >= query) return table[i][value]
                i++
            }
        } else
            while (i >= 0) {
                if (table[i][key] <= query) return table[i][value]
                i--
            }

        return 0
    }
}
