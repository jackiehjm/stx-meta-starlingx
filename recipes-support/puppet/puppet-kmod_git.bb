SUMMARY = "Manage Linux kernel modules with Puppet"

LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://LICENSE;md5=0e5ccf641e613489e66aa98271dbe798"

PV = "2.1.1"
SRC_REV = "ea03df0eff7b7e5faccb9c4e386d451301468f04"
PROTOCOL = "https"
BRANCH = "master"
S = "${WORKDIR}/git"

SRC_URI = "git://github.com/camptocamp/puppet-kmod;protocol=${PROTOCOL};rev=${SRC_REV};branch=${BRANCH} \
	file://puppet-kmod/Add-gemspec.patch \
	"

inherit ruby

DEPENDS += " \
	ruby \
	facter \
	"

RDEPENDS_${PN} += " \
	ruby \
	facter \
	puppet \
	"

RUBY_INSTALL_GEMS = "puppet-kmod-${PV}.gem"

do_install_append() {
	: 
}
