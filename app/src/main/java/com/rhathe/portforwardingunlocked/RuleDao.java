package com.rhathe.portforwardingunlocked;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;


@Dao
public interface RuleDao {
	@Query("SELECT * FROM rule")
	LiveData<List<Rule>> getAll();

	@Query("SELECT * FROM rule WHERE is_enabled = 1")
	LiveData<List<Rule>> getAllEnabled();

	@Query("SELECT * FROM rule WHERE is_enabled = 1")
	List<Rule> getAllEnabledSync();

	@Query("SELECT * FROM rule WHERE uid = :uid")
	Rule getById(String uid);

	@Insert
	void insert(Rule rule);

	@Insert
	void insertAll(Rule... rules);

	@Delete
	void delete(Rule rule);
}
