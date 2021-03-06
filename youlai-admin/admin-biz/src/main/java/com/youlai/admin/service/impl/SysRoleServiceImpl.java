package com.youlai.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.youlai.admin.entity.SysRole;
import com.youlai.admin.entity.SysRoleMenu;
import com.youlai.admin.entity.SysRoleResource;
import com.youlai.admin.entity.SysUserRole;
import com.youlai.admin.mapper.SysRoleMapper;
import com.youlai.admin.service.ISysRoleMenuService;
import com.youlai.admin.service.ISysRoleResourceService;
import com.youlai.admin.service.ISysRoleService;
import com.youlai.admin.service.ISysUserRoleService;
import com.youlai.common.web.exception.BizException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements ISysRoleService {

    private ISysRoleMenuService iSysRoleMenuService;
    private ISysUserRoleService iSysUserRoleService;
    private ISysRoleResourceService iSysRoleResourceService;

    @Override
    public boolean add(SysRole role) {
        List<Integer> menuIds = role.getMenuIds();
        Integer roleId = role.getId();
        List<SysRoleMenu> roleMenus = new ArrayList<>();
        Optional.ofNullable(menuIds).ifPresent(list -> list.stream().forEach(menuId ->
                roleMenus.add(new SysRoleMenu().setRoleId(roleId).setMenuId(menuId)))
        );
        iSysRoleMenuService.saveBatch(roleMenus);
        return this.save(role);
    }


    @Override
    public boolean update(SysRole role) {
        Integer roleId = role.getId();
        List<Integer> menuIds = role.getMenuIds();
        List<Integer> dbMenuIds = iSysRoleMenuService.list(new LambdaQueryWrapper<SysRoleMenu>()
                .eq(SysRoleMenu::getRoleId, roleId)).stream()
                .map(item -> item.getMenuId()).collect(Collectors.toList()); // 数据库角色拥有菜单权限ID

        // 删除角色菜单
        Optional.ofNullable(dbMenuIds).filter(item -> menuIds == null || !menuIds.contains(item))
                .ifPresent(list -> list.stream()
                        .forEach(menuId -> iSysRoleMenuService.remove(new LambdaQueryWrapper<SysRoleMenu>()
                                .eq(SysRoleMenu::getRoleId, roleId)
                                .eq(SysRoleMenu::getMenuId, menuId)
                        )));

        // 新增角色菜单
        Optional.ofNullable(menuIds).filter(item -> dbMenuIds == null || !dbMenuIds.contains(item))
                .ifPresent(list -> list.stream()
                        .forEach(menuId -> {
                            iSysRoleMenuService.save(new SysRoleMenu().setRoleId(roleId).setMenuId(menuId));
                        }));
        return true;
    }

    @Override
    public boolean delete(List<Integer> ids) {
        Optional.ofNullable(ids).orElse(new ArrayList<>()).forEach(id -> {
            int count = iSysUserRoleService.count(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, id));
            if (count > 0) {
                throw new BizException("该角色已分配用户，无法删除");
            }
            iSysRoleMenuService.remove(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, id));
        });

        return this.removeByIds(ids);
    }

    @Override
    public boolean update(Integer roleId, List<Integer> resourceIds) {
        List<Integer> dbResourceIds = iSysRoleResourceService.list(
                new LambdaQueryWrapper<SysRoleResource>()
                        .eq(SysRoleResource::getRoleId, roleId)).stream().map(item -> item.getResourceId()).collect(Collectors.toList());

        // 删除角色资源
        Optional.ofNullable(dbResourceIds)
                .filter(item -> resourceIds == null || !resourceIds.contains(item))
                .ifPresent(list -> list.stream().forEach(resourceId ->
                        iSysRoleResourceService.remove(new LambdaQueryWrapper<SysRoleResource>()
                                .eq(SysRoleResource::getRoleId, roleId)
                                .eq(SysRoleResource::getResourceId, resourceId)))
                );

        // 新增角色资源
        Optional.ofNullable(resourceIds)
                .filter(item -> dbResourceIds == null || !dbResourceIds.contains(item))
                .ifPresent(list -> list.stream().forEach(resourceId -> {
                    iSysRoleResourceService.save(new SysRoleResource().setRoleId(roleId).setResourceId(resourceId));
                }));
        return true;
    }
}
