package DataConvertor.das;

import DataConvertor.dao.FileDao;
import DataConvertor.model.FileEntity;
import DataConvertor.repository.FileRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class FileDataAccessService implements FileDao {
    FileRepository fileRepository;

    public FileDataAccessService(FileRepository fileRepository) {
        this.fileRepository = fileRepository;
    }

    @Override
    public void save(FileEntity file) {
        fileRepository.save(file);

    }
    @Override
    public Optional<FileEntity> getFileById(Long id) {
        return fileRepository.findById(id);
    }

    @Override
    public List<FileEntity> getAllFiles() {
        return fileRepository.findAll();
    }
}