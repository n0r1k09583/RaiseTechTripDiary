package com.raisetech.tripdiary.mapper;

import com.raisetech.tripdiary.domain.Post;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PostMapper {

  Post findById(@Param("id") long id);

  Post findForViewer(@Param("id") long id, @Param("viewerId") long viewerId);

  List<Post> list(
      @Param("viewerId") long viewerId,
      @Param("tab") String tab,
      @Param("area") String area,
      @Param("limit") int limit,
      @Param("beforeCreatedAt") String beforeCreatedAt,
      @Param("beforeId") Long beforeId,
      @Param("afterCreatedAt") String afterCreatedAt,
      @Param("afterId") Long afterId);

  List<Post> listByAuthor(
      @Param("viewerId") long viewerId,
      @Param("authorId") long authorId,
      @Param("limit") int limit,
      @Param("beforeCreatedAt") String beforeCreatedAt,
      @Param("beforeId") Long beforeId,
      @Param("afterCreatedAt") String afterCreatedAt,
      @Param("afterId") Long afterId);

  int insert(Post post);

  int update(Post post);

  int deleteById(@Param("id") long id);

  int count();

  int setImagePathIfBlank(@Param("spotName") String spotName, @Param("imagePath") String imagePath);
}
