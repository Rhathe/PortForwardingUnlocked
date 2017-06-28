package com.rhathe.portforwardingunlocked;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;


@Dao
public interface RuleDao {
	@Query("SELECT * FROM rule")
	List<Rule> getAll();

	@Query("SELECT * FROM rule WHERE is_enabled = 1")
	List<Rule> getAllEnabled();

	@Insert
	void insertAll(Rule... rules);

	@Delete
	void delete(Rule rule);
}
