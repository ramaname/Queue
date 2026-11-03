package by.bsuir.queue.lab.repository;

import by.bsuir.queue.lab.model.LabPass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface LabPassRepository extends JpaRepository<LabPass, Long> {

    int countByUserIdAndSubjectId(Long userId, Long subjectId);

    /** Сколько лаб по предмету сдал каждый из студентов: строки [userId, count]. */
    @Query("select lp.user.id, count(lp) from LabPass lp "
            + "where lp.subject.id = :subjectId and lp.user.id in :userIds group by lp.user.id")
    List<Object[]> countByUsers(@Param("subjectId") Long subjectId, @Param("userIds") Collection<Long> userIds);
}
