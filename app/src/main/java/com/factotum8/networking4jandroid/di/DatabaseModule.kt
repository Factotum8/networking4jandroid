package com.factotum8.networking4jandroid.di

import android.content.Context
import androidx.room.Room
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
import com.factotum8.networking4jandroid.data.local.db.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DATABASE_NAME).build()

    @Provides
    fun provideContactDao(database: AppDatabase): ContactDao = database.contactDao()

    @Provides
    fun provideRelativeDao(database: AppDatabase): RelativeDao = database.relativeDao()

    @Provides
    fun provideCompanyDao(database: AppDatabase): CompanyDao = database.companyDao()

    @Provides
    fun provideEventDao(database: AppDatabase): EventDao = database.eventDao()

    @Provides
    fun provideCommunityDao(database: AppDatabase): CommunityDao = database.communityDao()

    @Provides
    fun provideProjectDao(database: AppDatabase): ProjectDao = database.projectDao()

    @Provides
    fun provideInterestDao(database: AppDatabase): InterestDao = database.interestDao()

    @Provides
    fun provideTagDao(database: AppDatabase): TagDao = database.tagDao()

    @Provides
    fun provideContactCompanyCrossRefDao(database: AppDatabase): ContactCompanyCrossRefDao =
        database.contactCompanyCrossRefDao()

    @Provides
    fun provideContactEventCrossRefDao(database: AppDatabase): ContactEventCrossRefDao =
        database.contactEventCrossRefDao()

    @Provides
    fun provideContactCommunityCrossRefDao(database: AppDatabase): ContactCommunityCrossRefDao =
        database.contactCommunityCrossRefDao()

    @Provides
    fun provideContactProjectCrossRefDao(database: AppDatabase): ContactProjectCrossRefDao =
        database.contactProjectCrossRefDao()

    @Provides
    fun provideContactInterestCrossRefDao(database: AppDatabase): ContactInterestCrossRefDao =
        database.contactInterestCrossRefDao()

    @Provides
    fun provideContactTagCrossRefDao(database: AppDatabase): ContactTagCrossRefDao =
        database.contactTagCrossRefDao()

    @Provides
    fun provideKnowsDao(database: AppDatabase): KnowsDao = database.knowsDao()
}
