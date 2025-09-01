package com.refit.app.domain.order.mapper;

import com.refit.app.domain.order.dto.AddressRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AddressMapper {

    AddressRow findDefaultByUserId(@Param("memberId") long memberId);
}
