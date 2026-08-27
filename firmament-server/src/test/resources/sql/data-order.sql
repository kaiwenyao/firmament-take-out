-- Address book for user 100 (POST /user/addressBook does not return the new id).
INSERT INTO `address_book`
(`id`, `user_id`, `consignee`, `phone`, `sex`,
 `province_code`, `province_name`, `city_code`, `city_name`,
 `district_code`, `district_name`, `detail`, `label`, `is_default`)
VALUES
(1000, 100, 'Zhang San', '13900000000', '1',
 '110000', 'Beijing', '110100', 'Beijing',
 '110105', 'Chaoyang', 'IT-address-1', 'home', 1);
