package com.cjlu.finalversionwebsystem.mapper;

import com.cjlu.finalversionwebsystem.entity.ProfilePicture;
import org.apache.ibatis.annotations.*;

@Mapper
public interface ProfilePictureMapper {

    @Insert("CREATE TABLE IF NOT EXISTS profile_picture (id INT AUTO_INCREMENT PRIMARY KEY, profile_picture_location VARCHAR(255), userName VARCHAR(255))")
    void createTable();

    @Insert("INSERT INTO profile_picture (profile_picture_location, userName) VALUES (#{profilePictureLocation}, #{userName})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertProfilePicture(@Param("profilePictureLocation") String profilePictureLocation, @Param("userName") String userName);

    @Select("SELECT * FROM profile_picture WHERE userName = #{userName}")
    ProfilePicture selectProfilePictureByUserName(@Param("userName") String userName);

    @Delete("DELETE FROM profile_picture WHERE userName = #{userName}")
    int deleteProfilePictureByUserName(@Param("userName") String userName);

    @Update("UPDATE profile_picture SET profile_picture_location = #{profilePictureLocation} WHERE userName = #{userName}")
    int updateProfilePicture(@Param("profilePictureLocation") String profilePictureLocation, @Param("userName") String userName);
}
    

