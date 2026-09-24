package com.raisetech.tripdiary.mapper;

import com.raisetech.tripdiary.domain.Follow;
import com.raisetech.tripdiary.domain.UserSummary;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FollowMapper {

  Follow find(@Param("followerId") long followerId, @Param("followeeId") long followeeId);

  int insert(Follow follow);

  int delete(@Param("followerId") long followerId, @Param("followeeId") long followeeId);

  int count();

  List<UserSummary> listFollowees(@Param("userId") long userId, @Param("viewerId") long viewerId);

  List<UserSummary> listFollowers(@Param("userId") long userId, @Param("viewerId") long viewerId);
}
