package net.pyxzl.orayen.dto

import net.pyxzl.orayen.Config

import org.mindrot.jbcrypt.BCrypt


class UserDTO extends DTO {
	String name
	String password

	UserDTO(final String name, final String password, final boolean clearPass) {
		this.name = name
		this.password = clearPass ? encrypt(password) : password
	}

	static String encrypt(final String password) {
		BCrypt.hashpw(password, Config.Setting.BCRYPT_SALT.value)
	}
}
