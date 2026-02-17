-- 호텔 데이터
INSERT INTO hotel (id, name, address, description) VALUES
(1, '시그니엘 서울', '서울특별시 송파구 올림픽로 300 롯데월드타워 76~101층', '롯데월드타워 최상층에 위치한 초럭셔리 호텔'),
(2, '롯데호텔 제주', '제주특별자치도 서귀포시 중문관광로72번길 35', '중문관광단지 내 리조트형 호텔');

-- 객실 타입 데이터
INSERT INTO room_type (id, hotel_id, name, description, price, max_occupancy) VALUES
(1, 1, '그랜드 디럭스', '시티뷰 킹베드 객실 (43층 이상)', 350000.00, 2),
(2, 1, '프리미어 스위트', '리빙룸 포함 스위트 (50층 이상)', 750000.00, 3),
(3, 1, '로얄 스위트', '최상층 파노라마뷰 스위트', 1500000.00, 4),
(4, 2, '스탠다드 오션뷰', '바다 전망 기본 객실', 200000.00, 2),
(5, 2, '디럭스 오션뷰', '넓은 바다 전망 객실 (발코니 포함)', 320000.00, 3);

-- 재고 데이터: 오늘부터 30일간 각 객실 타입별 재고 생성
-- 시그니엘 서울 - 그랜드 디럭스 (10실)
INSERT INTO inventory (room_type_id, date, total_quantity, available_quantity)
SELECT 1, DATEADD('DAY', x, CURRENT_DATE), 10, 10
FROM SYSTEM_RANGE(0, 29) AS t(x);

-- 시그니엘 서울 - 프리미어 스위트 (5실)
INSERT INTO inventory (room_type_id, date, total_quantity, available_quantity)
SELECT 2, DATEADD('DAY', x, CURRENT_DATE), 5, 5
FROM SYSTEM_RANGE(0, 29) AS t(x);

-- 시그니엘 서울 - 로얄 스위트 (3실)
INSERT INTO inventory (room_type_id, date, total_quantity, available_quantity)
SELECT 3, DATEADD('DAY', x, CURRENT_DATE), 3, 3
FROM SYSTEM_RANGE(0, 29) AS t(x);

-- 롯데호텔 제주 - 스탠다드 오션뷰 (8실)
INSERT INTO inventory (room_type_id, date, total_quantity, available_quantity)
SELECT 4, DATEADD('DAY', x, CURRENT_DATE), 8, 8
FROM SYSTEM_RANGE(0, 29) AS t(x);

-- 롯데호텔 제주 - 디럭스 오션뷰 (4실)
INSERT INTO inventory (room_type_id, date, total_quantity, available_quantity)
SELECT 5, DATEADD('DAY', x, CURRENT_DATE), 4, 4
FROM SYSTEM_RANGE(0, 29) AS t(x);
