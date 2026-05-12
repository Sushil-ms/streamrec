package com.streamrec.repository;

import com.streamrec.entity.Track;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrackRepository extends JpaRepository<Track, String> {
}
