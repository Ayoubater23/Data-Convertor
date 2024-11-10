package DataConvertor.dao;

import DataConvertor.model.FileEntity;

import java.util.List;
import java.util.Optional;

public interface FileDao {
    public void  save(FileEntity file);
    public Optional<FileEntity> getFileById(Long id);
    public List<FileEntity> getAllFiles();
}