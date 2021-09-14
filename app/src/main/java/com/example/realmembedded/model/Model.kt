package com.example.realmembedded.model

import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass

open class Parent : RealmObject() {
    @PrimaryKey
    var id: Int = 0

    var children: RealmList<Child> = RealmList()
}

@RealmClass(embedded = true)
open class Child : RealmObject() {
    var code: String = ""
}