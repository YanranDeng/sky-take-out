package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.PasswordConstant;
import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.entity.Employee;
import com.sky.exception.AccountLockedException;
import com.sky.exception.AccountNotFoundException;
import com.sky.exception.PasswordErrorException;
import com.sky.mapper.EmployeeMapper;
import com.sky.result.PageResult;
import com.sky.service.EmployeeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class EmployeeServiceImpl implements EmployeeService {

    @Autowired
    private EmployeeMapper employeeMapper;

    /**
     * 员工登录
     *
     * @param employeeLoginDTO
     * @return
     */
    public Employee login(EmployeeLoginDTO employeeLoginDTO) {
        String username = employeeLoginDTO.getUsername();
        String password = employeeLoginDTO.getPassword();

        // 使用DigestUtils工具类对密码进行加密,得到的密文密码
        String encode = DigestUtils.md5DigestAsHex(password.getBytes());
        log.info(encode);
        //1、根据用户名查询数据库中的数据
        Employee employee = employeeMapper.getByUsername(username);

        //2、处理各种异常情况（用户名不存在、密码不对、账号被锁定）
        if (employee == null) {
            //账号不存在
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        //密码比对
        // 使用新生成的密文密码与数据库employee表中的密文密码比较
        if (!encode.equals(employee.getPassword())) {
            //密码错误
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }

        if (employee.getStatus() == StatusConstant.DISABLE) {
            //账号被锁定
            throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
        }

        //3、返回实体对象
        return employee;
    }

    /**
     * 添加员工
     *
     * @param employeeDTO
     */
    @Override
    public void save(EmployeeDTO employeeDTO) {
        // 新建员工实体类,该类和员工dto类的区别在于密码属性等后几个属性,
        // 之所以在Controller层接收参数时使用dto类而非实体类是因为要符合接口规范,精准接收.
        Employee employee = new Employee();
        // 然后在这里使用BeanUtils类中的copyProperties方法将原有的dto类的属性值复制到实体类中,
        // 由于dto的属性名和实体类的前面的属性值名一致,可以直接复制.
        // 在传给mapper层的参数就需要变更为实体类,这是因为数据表字段和实体类是对应的,
        // 而dto类只负责接收前端请求参数,而不负责将参数传递给mapper层.
        BeanUtils.copyProperties(employeeDTO, employee);

        // 补全实体类剩余属性
        // 使用md50算法加密默认密码,由于数据库中存放的也是md50加密后的密码,因此要两个统一才能比较
        String password = DigestUtils.md5DigestAsHex(PasswordConstant.DEFAULT_PASSWORD.getBytes());
        employee.setPassword(password);

        employee.setStatus(StatusConstant.ENABLE);

/*        employee.setCreateTime(LocalDateTime.now());
        employee.setUpdateTime(LocalDateTime.now());*/

        // 将在拦截器中的保存到线程内存的用户id提取出来
/*        Long empId = BaseContext.getCurrentId();
        employee.setCreateUser(empId);
        employee.setUpdateUser(empId);*/

        employeeMapper.insert(employee);
        
    }

    @Override
    public PageResult pageQuery(EmployeePageQueryDTO employeePageQueryDTO) {
        PageHelper.startPage(employeePageQueryDTO.getPage(), employeePageQueryDTO.getPageSize());
        Page<Employee> page = employeeMapper.pageQuery(employeePageQueryDTO);
        long total = page.getTotal();
        log.info(String.valueOf(total));
        List<Employee> result = page.getResult();
        log.info(result.toString());
        return new PageResult(total, result);

    }

    /**
     * 启用/禁用员工账号
     * @param status
     * @param id
     */
    @Override
    public void onOrClose(Integer status, Long id) {
        Employee employee = new Employee();
        employee.setId(id);
        employee.setStatus(status);

        employeeMapper.update(employee);
    }

    /**
     * 编辑员工信息,该方法需要在查询员工方法之后执行
     *
     * @param employeeDTO
     */
    @Override
    public void update(EmployeeDTO employeeDTO) {
        Employee employee = new Employee();
        BeanUtils.copyProperties(employeeDTO,employee);

//        employee.setUpdateTime(LocalDateTime.now());
        // 将线程内存存储的当前操作人的id取出,并赋值给employee对象.操作人id属性是在拦截器中通过TreadLocal对象存入的.
//        employee.setUpdateUser(BaseContext.getCurrentId());

        employeeMapper.update(employee);
    }

    @Override
    public Employee getById(Long id) {
        Employee employee = employeeMapper.getById(id);
        employee.setPassword("******");
        return employee;
    }

}
