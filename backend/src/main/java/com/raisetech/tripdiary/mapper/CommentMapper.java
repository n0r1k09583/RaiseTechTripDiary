package com.raisetech.tripdiary.mapper;

import com.raisetech.tripdiary.domain.Comment;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CommentMapper {

  Comment findById(@Param("id") long id);

  List<Comment> listByPostId(@Param("postId") long postId);

  int insert(Comment comment);

  int deleteById(@Param("id") long id);

  int deleteByPostId(@Param("postId") long postId);

  int count();
}
