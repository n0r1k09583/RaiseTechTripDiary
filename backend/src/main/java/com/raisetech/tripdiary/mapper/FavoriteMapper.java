package com.raisetech.tripdiary.mapper;

import com.raisetech.tripdiary.domain.Favorite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FavoriteMapper {

  Favorite find(@Param("postId") long postId, @Param("userId") long userId);

  int insert(Favorite favorite);

  int delete(@Param("postId") long postId, @Param("userId") long userId);

  int deleteByPostId(@Param("postId") long postId);
}
