package by.bsuir.queue.lab.repository;
import by.bsuir.queue.lab.model.LabPass;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LabPassRepository extends JpaRepository<LabPass, Long> {
    int countByUserIdAndSubjectId(Long userId, Long subjectId);
}