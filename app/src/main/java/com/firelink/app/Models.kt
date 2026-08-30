package com.firelink.app
data class Incident(val id:String="", val latitude:Double=0.0, val longitude:Double=0.0, val accuracyM:Double=0.0, val note:String="", val creatorUid:String="", val unitName:String="", val createdAt:Long=0L, val status:String="ACTIVE")
data class Ack(val unitName:String="", val at:Long=0L)

data class JoinRequest(val uid:String="", val displayName:String="", val email:String="", val teamId:String="", val createdAt:Long=0L)
