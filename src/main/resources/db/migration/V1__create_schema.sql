-- 호텔 테이블
CREATE TABLE hotel (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,              -- 호텔명
    address VARCHAR(500) NOT NULL,           -- 주소
    description VARCHAR(1000),               -- 호텔 설명
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 객실 타입 테이블
CREATE TABLE room_type (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    hotel_id BIGINT NOT NULL,                -- 소속 호텔 FK
    name VARCHAR(255) NOT NULL,              -- 룸 타입명 (예: 그랜드 디럭스)
    description VARCHAR(1000),               -- 룸 타입 설명
    price DECIMAL(12, 2) NOT NULL,           -- 1박 가격
    max_occupancy INT NOT NULL,              -- 최대 투숙 인원
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_room_type_hotel FOREIGN KEY (hotel_id) REFERENCES hotel(id)
);

-- 재고 테이블: 룸타입별 + 날짜별 객실 재고
CREATE TABLE inventory (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_type_id BIGINT NOT NULL,            -- 룸 타입 FK
    date DATE NOT NULL,                      -- 재고 날짜
    total_quantity INT NOT NULL,             -- 전체 객실 수
    available_quantity INT NOT NULL,         -- 잔여 객실 수 (예약 시 차감, 취소 시 복원)
    version BIGINT NOT NULL DEFAULT 0,       -- Optimistic Lock용 버전 (동시 수정 감지)
    CONSTRAINT fk_inventory_room_type FOREIGN KEY (room_type_id) REFERENCES room_type(id),
    CONSTRAINT uk_inventory_room_type_date UNIQUE (room_type_id, date) -- 동일 룸타입+날짜 중복 방지
);

-- 예약 테이블
CREATE TABLE reservation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_type_id BIGINT NOT NULL,            -- 룸 타입 FK
    guest_name VARCHAR(255) NOT NULL,        -- 투숙객 이름
    guest_email VARCHAR(255) NOT NULL,       -- 투숙객 이메일
    check_in_date DATE NOT NULL,             -- 체크인 날짜 (포함)
    check_out_date DATE NOT NULL,            -- 체크아웃 날짜 (미포함)
    status VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED', -- 예약 상태 (CONFIRMED → CANCELLED)
    number_of_rooms INT NOT NULL,            -- 예약 객실 수
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reservation_room_type FOREIGN KEY (room_type_id) REFERENCES room_type(id)
);
