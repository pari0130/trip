-- guest 테이블 생성
CREATE TABLE guest (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 기존 reservation 데이터에서 guest 레코드 생성 (데이터 무결성)
INSERT INTO guest (name, email)
SELECT DISTINCT guest_name, guest_email FROM reservation;

-- reservation에 guest_id FK 컬럼 추가
ALTER TABLE reservation ADD COLUMN guest_id BIGINT;

-- 기존 데이터 마이그레이션
UPDATE reservation r SET r.guest_id = (
    SELECT g.id FROM guest g WHERE g.email = r.guest_email
);

-- NOT NULL 제약 + FK 추가
ALTER TABLE reservation ALTER COLUMN guest_id SET NOT NULL;
ALTER TABLE reservation ADD CONSTRAINT fk_reservation_guest FOREIGN KEY (guest_id) REFERENCES guest(id);

-- 기존 컬럼 제거
ALTER TABLE reservation DROP COLUMN guest_name;
ALTER TABLE reservation DROP COLUMN guest_email;
