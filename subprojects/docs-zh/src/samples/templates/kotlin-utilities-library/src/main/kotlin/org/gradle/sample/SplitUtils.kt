package org.gradle.sample

class SplitUtils {
    companion object {
        fun split(source: String): LinkedList {
            var lastFind = 0
            val result = LinkedList()

            var currentFind = source.indexOf(" ", lastFind)
            while (currentFind != -1) {
                var token = source.substring(lastFind)
                if (currentFind != -1) {
                    token = token.substring(0, currentFind - lastFind)
                }

                addIfValid(token, result)
                lastFind = currentFind + 1
                currentFind = source.indexOf(" ", lastFind)
            }

            val token = source.substring(lastFind)
            addIfValid(token, result)

            return result
        }

        private fun addIfValid(token: String, list: LinkedList) {
            if (isTokenValid(token)) {
                list.add(token)
            }
        }

        private fun isTokenValid(token: String): Boolean {
            return !token.isEmpty()
        }
    }
}