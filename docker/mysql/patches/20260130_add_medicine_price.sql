-- Add price fields for medicine and prescription items
ALTER TABLE medicines
  ADD COLUMN price DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '单价';

ALTER TABLE prescription_items
  ADD COLUMN unit_price DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '单价',
  ADD COLUMN total_price DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '小计';
