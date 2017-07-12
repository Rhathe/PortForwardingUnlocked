package com.rhathe.portforwardingunlocked;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

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

	@Query("SELECT * FROM rule WHERE uid != :uid AND :from <= from_port + port_range AND :end >= from_port")
	Rule getOverlappingRule(int uid, int from, int end);

	@Insert
	void insert(Rule rule);

	@Update
	void update(Rule rule);

	@Insert
	void insertAll(Rule... rules);

	@Delete
	void delete(Rule rule);
}
