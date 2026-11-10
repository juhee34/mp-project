package com.tinyrunners.school.stories

import android.content.SharedPreferences
import java.lang.reflect.Proxy
import org.junit.Assert.*
import org.junit.Test

class StoryStoreTest {
    private class MemoryPreferences {
        val values = mutableMapOf<String, Any>()
        var commitSucceeds = true
        val prefs = Proxy.newProxyInstance(SharedPreferences::class.java.classLoader,
            arrayOf(SharedPreferences::class.java)) { _, method, args ->
            when (method.name) {
                "getStringSet" -> (values[args[0]] ?: args[1]) as Set<*>
                "getInt" -> (values[args[0]] ?: args[1]) as Int
                "edit" -> editor()
                else -> error("Unexpected preference operation: ${method.name}")
            }
        } as SharedPreferences

        private fun editor(): SharedPreferences.Editor {
            val updates = mutableMapOf<String, Any?>()
            return Proxy.newProxyInstance(SharedPreferences.Editor::class.java.classLoader,
                arrayOf(SharedPreferences.Editor::class.java)) { proxy, method, args ->
                when (method.name) {
                    "putInt", "putStringSet" -> { updates[args[0] as String] = args[1]; proxy }
                    "remove" -> { updates[args[0] as String] = null; proxy }
                    "commit" -> {
                        if (commitSucceeds) updates.forEach { (key, value) ->
                            if (value == null) values.remove(key) else values[key] = value
                        }
                        commitSucceeds
                    }
                    else -> error("Unexpected editor operation: ${method.name}")
                }
            } as SharedPreferences.Editor
        }
    }

    @Test fun accountNamespacesCannotAliasGuestOrAnotherUser() {
        assertEquals("story_reading_v1_guest", StoryStore.preferenceName(null))
        assertEquals("story_reading_v1_user_alice", StoryStore.preferenceName("alice"))
        assertEquals(3, setOf(StoryStore.preferenceName(null), StoryStore.preferenceName("guest"),
            StoryStore.preferenceName("alice")).size)
    }

    @Test fun bookmarksSurviveReopeningWithoutMarkingRead() {
        val disk = MemoryPreferences()
        val store = StoryStore(disk.prefs)
        assertTrue(store.unlock("mint_episode_1"))
        assertTrue(store.savePage("mint_episode_1", 7))
        val reopened = StoryStore(disk.prefs)
        assertEquals(7, reopened.page("mint_episode_1"))
        assertTrue("mint_episode_1" in reopened.unlocked)
        assertTrue(reopened.read.isEmpty())
    }

    @Test fun explicitCompletionClearsBookmarkAndKeepsUnlock() {
        val store = StoryStore(MemoryPreferences().prefs)
        store.savePage("mint_intro", 2)
        assertTrue(store.markRead("mint_intro"))
        assertEquals(0, store.page("mint_intro"))
        assertTrue("mint_intro" in store.read && "mint_intro" in store.unlocked)
        store.savePage("mint_intro", 1)
        assertTrue("mint_intro" in store.read)
    }

    @Test fun accountsAndCharactersKeepSeparateBookmarksAndReadFlags() {
        val accountA = StoryStore(MemoryPreferences().prefs)
        val accountB = StoryStore(MemoryPreferences().prefs)
        accountA.markRead("mint_intro")
        accountA.savePage("cheese_intro", 1)
        assertTrue(accountB.read.isEmpty())
        assertEquals(0, accountB.page("cheese_intro"))
        assertEquals(0, accountA.page("mint_intro"))
        assertEquals(1, accountA.page("cheese_intro"))
    }

    @Test fun storageErrorsAreReturnedToTheScreen() {
        val disk = MemoryPreferences().apply { commitSucceeds = false }
        val store = StoryStore(disk.prefs)
        assertFalse(store.unlock("mint_intro"))
        assertFalse(store.savePage("mint_intro", 1))
        assertFalse(store.markRead("mint_intro"))
    }
}
