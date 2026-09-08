USE cloud_hospital;
INSERT INTO patient (patient_no,id_card,name,gender,birthday,phone,address) VALUES
('P202609080001','410101200408101234','王小明','MALE','2004-08-10','13800000001','河南省郑州市'),
('P202609080002','410101200503152345','李晓雨','FEMALE','2005-03-15','13800000002','河南省信阳市');
INSERT INTO doctor (doctor_no,login_name,password_hash,real_name,department_name,title,enabled) VALUES
('D001','zhangwei','{noop}123456','张伟','内科','主治医师',1),
('D002','lihua','{noop}123456','李华','外科','副主任医师',1);
INSERT INTO registration (visit_no,patient_id,doctor_id,visit_date,registration_fee,status,registered_at) VALUES ('V202609080001',1,1,CURDATE(),8.00,'WAITING',NOW());
