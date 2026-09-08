package com.example.cloudhospital.prescription;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
@Mapper
public interface PrescriptionItemRepository {
    List<PrescriptionItem> findByPrescriptionId(Long prescriptionId);
    int saveAll(List<PrescriptionItem> items);
}
