package com.raisetech.tripdiary.mapper;

import com.raisetech.tripdiary.domain.RefreshToken;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RefreshTokenMapper {

  RefreshToken findValidByHash(@Param("tokenHash") String tokenHash, @Param("now") long now);

  int insert(RefreshToken token);

  int deleteById(@Param("id") long id);

  int deleteByHash(@Param("tokenHash") String tokenHash);
}
