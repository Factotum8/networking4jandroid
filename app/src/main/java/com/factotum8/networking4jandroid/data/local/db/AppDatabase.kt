package com.factotum8.networking4jandroid.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.factotum8.networking4jandroid.data.local.dao.CommunityDao
import com.factotum8.networking4jandroid.data.local.dao.CompanyDao
import com.factotum8.networking4jandroid.data.local.dao.ContactCommunityCrossRefDao
import com.factotum8.networking4jandroid.data.local.dao.ContactCompanyCrossRefDao
import com.factotum8.networking4jandroid.data.local.dao.ContactDao
import com.factotum8.networking4jandroid.data.local.dao.ContactEventCrossRefDao
import com.factotum8.networking4jandroid.data.local.dao.ContactInterestCrossRefDao
import com.factotum8.networking4jandroid.data.local.dao.ContactProjectCrossRefDao
import com.factotum8.networking4jandroid.data.local.dao.ContactTagCrossRefDao
import com.factotum8.networking4jandroid.data.local.dao.EventDao
import com.factotum8.networking4jandroid.data.local.dao.InterestDao
import com.factotum8.networking4jandroid.data.local.dao.KnowsDao
import com.factotum8.networking4jandroid.data.local.dao.ProjectDao
import com.factotum8.networking4jandroid.data.local.dao.RelativeDao
import com.factotum8.networking4jandroid.data.local.dao.TagDao
import com.factotum8.networking4jandroid.data.local.entity.CommunityEntity
import com.factotum8.networking4jandroid.data.local.entity.CompanyEntity
import com.factotum8.networking4jandroid.data.local.entity.ContactCommunityCrossRef
import com.factotum8.networking4jandroid.data.local.entity.ContactCompanyCrossRef
import com.factotum8.networking4jandroid.data.local.entity.ContactEntity
import com.factotum8.networking4jandroid.data.local.entity.ContactEventCrossRef
import com.factotum8.networking4jandroid.data.local.entity.ContactInterestCrossRef
import com.factotum8.networking4jandroid.data.local.entity.ContactProjectCrossRef
import com.factotum8.networking4jandroid.data.local.entity.ContactTagCrossRef
import com.factotum8.networking4jandroid.data.local.entity.EventEntity
import com.factotum8.networking4jandroid.data.local.entity.InterestEntity
import com.factotum8.networking4jandroid.data.local.entity.KnowsEntity
import com.factotum8.networking4jandroid.data.local.entity.ProjectEntity
import com.factotum8.networking4jandroid.data.local.entity.RelativeEntity
import com.factotum8.networking4jandroid.data.local.entity.TagEntity

@Database(
    entities = [
        ContactEntity::class,
        RelativeEntity::class,
        CompanyEntity::class,
        EventEntity::class,
        CommunityEntity::class,
        ProjectEntity::class,
        InterestEntity::class,
        TagEntity::class,
        ContactCompanyCrossRef::class,
        ContactEventCrossRef::class,
        ContactCommunityCrossRef::class,
        ContactProjectCrossRef::class,
        ContactInterestCrossRef::class,
        ContactTagCrossRef::class,
        KnowsEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
    abstract fun relativeDao(): RelativeDao
    abstract fun companyDao(): CompanyDao
    abstract fun eventDao(): EventDao
    abstract fun communityDao(): CommunityDao
    abstract fun projectDao(): ProjectDao
    abstract fun interestDao(): InterestDao
    abstract fun tagDao(): TagDao
    abstract fun contactCompanyCrossRefDao(): ContactCompanyCrossRefDao
    abstract fun contactEventCrossRefDao(): ContactEventCrossRefDao
    abstract fun contactCommunityCrossRefDao(): ContactCommunityCrossRefDao
    abstract fun contactProjectCrossRefDao(): ContactProjectCrossRefDao
    abstract fun contactInterestCrossRefDao(): ContactInterestCrossRefDao
    abstract fun contactTagCrossRefDao(): ContactTagCrossRefDao
    abstract fun knowsDao(): KnowsDao

    companion object {
        const val DATABASE_NAME = "networking4j.db"
    }
}
