package com.example.cloudhospital.prescription;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface PrescriptionItemRepository {
  List<PrescriptionItem> findByPrescriptionId(Long prescriptionId);

  int saveAll(List<PrescriptionItem> items);
}
