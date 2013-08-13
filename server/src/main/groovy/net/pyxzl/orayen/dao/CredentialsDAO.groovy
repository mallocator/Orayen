package net.pyxzl.orayen.dao

import javax.security.auth.x500.X500PrivateCredential

import net.pyxzl.orayen.Config.Setting
import net.pyxzl.orayen.service.EsService

@Singleton
class CredentialsDAO {
	private static final String ES_TYPE = 'ssl'

	X500PrivateCredential get(String alias) {
		def cred = EsService.instance.client.get {
			index Setting.ES_INDEX.value
			type ES_TYPE
			id alias
		}
		if (cred.response.exists) {
			return new X500PrivateCredential(bytea2obj(cred.response.source.cert), bytea2obj(cred.response.source.key), cred.response.source.alias)
		}
		null
	}

	X500PrivateCredential put(X500PrivateCredential cred) {
		EsService.instance.client.index {
			index Setting.ES_INDEX.value
			type ES_TYPE
			id cred.alias
			source {
				alias = cred.alias
				cert = obj2bytea(cred.cert)
				key = obj2bytea(cred.key)
			}
		}
		cred
	}

	void delete(String alias) {
		EsService.instance.client.delete {
			index Setting.ES_INDEX.value
			type ES_TYPE
			id alias
		}
	}

	private byte[] obj2bytea(Object o) {
		final ByteArrayOutputStream bos = new ByteArrayOutputStream()
		ObjectOutput output = null
		try {
			output = new ObjectOutputStream(bos)
			output.writeObject(o)
			return bos.toByteArray()
		} finally {
			output.close()
			bos.close()
		}
	}

	private Object bytea2obj(byte[] ba) {
		final ByteArrayInputStream bis = new ByteArrayInputStream(ba)
		ObjectInput input = null
		try {
			input = new ObjectInputStream(bis)
			return input.readObject()
		} finally {
			bis.close()
			input.close()
		}
	}
}
