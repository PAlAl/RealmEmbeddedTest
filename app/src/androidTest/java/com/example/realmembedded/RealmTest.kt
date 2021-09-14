package com.example.realmembedded

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.realmembedded.model.Child
import com.example.realmembedded.model.Parent
import io.realm.Realm
import io.realm.RealmList
import io.realm.RealmModel
import io.realm.RealmQuery
import io.realm.kotlin.delete
import io.realm.kotlin.where
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch

@RunWith(AndroidJUnit4::class)
class RealmTest {

    private fun execute(executeBlock: (realm: Realm) -> Unit) {
        Realm.getDefaultInstance().use { realm ->
            realm.executeTransaction {
                executeBlock(it)
            }
        }
    }

    private fun <T : RealmModel> detachedFindAll(executeBlock: (realm: Realm) -> RealmQuery<T>): List<T> {
        var result: List<T>

        Realm.getDefaultInstance().use { realm ->
            result = realm.copyFromRealm(executeBlock(realm).findAll())
        }

        return result
    }

    @Test
    fun test() {
        val scenario: ActivityScenario<MainActivity>? =
            ActivityScenario.launch(MainActivity::class.java)

        val setupLatch = CountDownLatch(1)

        scenario?.onActivity { activity: MainActivity ->
            Realm.init(activity)
            setupLatch.countDown()
        }

        /*** Init Model ***/
        val parentIdChange = 1
        val parentIdWrong = 3

        val parents = listOf(
            Parent().apply {
                id = parentIdChange
                this.children = RealmList(
                    Child().apply {
                        code = "1"
                    },
                    Child().apply {
                        code = "11"
                    }
                )
            },

            Parent().apply {
                id = 2
            },

            Parent().apply {
                id = parentIdWrong
                this.children = RealmList(
                    Child().apply {
                        code = "3"
                    }
                )
            },
        )

        /*** Database Clear ***/
        execute {
            it.delete<Parent>()
        }
        /*** Check Clearing ***/
        var result: List<Parent> = detachedFindAll { it.where() }
        assertTrue(result.isEmpty())

        /*** Database Insert Model ***/
        execute { it.insertOrUpdate(parents) }
        /*** Check Insertion ***/
        result = detachedFindAll { it.where() }
        assertTrue(result.size == 3)
        assertTrue(result.first { it.id == parentIdChange }.children.size == 2)
        assertTrue(result.first { it.id == parentIdWrong }.children.size == 1)

        /*** Change Embedded Model in parent with parentIdChange id ***/
        result.first { it.id == parentIdChange }.children.first()?.code = "1234"

        /*** Database Update Model ***/
        execute { realm -> realm.insertOrUpdate(result) }

        /*** Check Updating ***/
        result = detachedFindAll { it.where() }
        assertTrue(result.size == 3)
        assertTrue(result.first { it.id == parentIdChange }.children.size == 2)
        /*** AssertionError: we have duplicate child in parent with parentIdWrong id ***/
        assertTrue(result.first { it.id == parentIdWrong }.children.size == 1)
    }
}