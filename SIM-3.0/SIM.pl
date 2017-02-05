#!/usr/bin/perl

#    SIM.pl : print the phone book of a GSM SIM card
#    Copyright (C) 2001,2004  Ludovic ROUSSEAU 
#
#    This program is free software; you can redistribute it and/or modify
#    it under the terms of the GNU General Public License as published by
#    the Free Software Foundation; either version 2 of the License, or
#    (at your option) any later version.
#
#    This program is distributed in the hope that it will be useful,
#    but WITHOUT ANY WARRANTY; without even the implied warranty of
#    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#    GNU General Public License for more details.
#
#    You should have received a copy of the GNU General Public License
#    along with this program; if not, write to the Free Software
#    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307 USA

use strict;
use warnings;
use Chipcard::PCSC;
use Chipcard::PCSC::Card;
use vars qw($opt_d $opt_D);
use Getopt::Std;
use Term::ReadKey;

# functions prototypes
sub pad_pin($);
sub SIM_phonebook_record($$);

my ($scr, $atr, $i, $pin, $rec, $rec_size);
my ($hContext, @readers_list, $hCard, @atr);
my ($sw, $data);


# -d to debug, -D to dump
getopts('dD');

# $pin = "31323334FFFFFFFF";

#$rec = SIM_phonebook_record("4C 75 64 6F FF FF FF FF FF FF FF FF FF FF 06 A8 10 32 54 76 98 FF FF FF FF FF FF FF", hex "1C");
#$rec = SIM_phonebook_record("56 45 52 4F 20 5A 45 43 FF FF FF FF FF FF FF FF FF FF FF FF 06 81 60 68 12 19 11 FF FF FF FF FF FF FF", hex "22");
#print "$rec\n"; exit;

######################   new    ####################
# allocate a descriptor
$hContext = new Chipcard::PCSC or die "Can't bind Chipcard::PCSC: $!\n";

######################   Readers    ####################
# Get the list of available readers
@readers_list = $hContext -> ListReaders;
die ("Can't get readers list: $Chipcard::PCSC::errno\n") unless defined
$readers_list[0];

print "No reader given: using $readers_list[0]\n" if ($opt_d);

######################   Card    ####################
# create a card association
$hCard = new Chipcard::PCSC::Card ($hContext, $readers_list[0]);
die ("Can't allocate Chipcard::PCSC::Card object: $Chipcard::PCSC::errno\n") unless defined $hCard;

# explicitely call exit when CTRL-C
$SIG{INT} = sub { exit };

######################   Get the ATR   ####################
if ($opt_D)
{
	my @status;

	@status = $hCard -> Status;
	print "ATR: " . Chipcard::PCSC::array_to_ascii($status[3]) . "\n";
}

######################   ResetCard   ####################
# Select MF 3F00
# sw = 9F16 or 9F1A
($sw, $data) = $hCard->TransmitWithCheck("A0 A4 00 00 02 3F 00", "9F ??", $opt_D);
die "Select MF: $Chipcard::PCSC::Card::Error" unless defined $sw;

# Select DF Telecom (7F10)
# sw = 9F16 or 9F1A
($sw, $data) = $hCard->TransmitWithCheck("A0 A4 00 00 02 7F 10", "9F ??", $opt_D);
die "Select DF Telecom: $Chipcard::PCSC::Card::Error" unless defined $sw;

# Select EF_ADN (6F3A) (Abbreviated Dialing Numbers) p62
($sw, $data) = $hCard->TransmitWithCheck("A0 A4 00 00 02 6F 3A", "9F 0F", $opt_D);
die "Select EF_ADN: $Chipcard::PCSC::Card::Error" unless defined $sw;

# Get Reponse
($sw, $rec) = $hCard->TransmitWithCheck("A0 C0 00 00 0F", "90 00", $opt_D);
die "Get Response: $Chipcard::PCSC::Card::Error" unless defined $sw;
$rec_size = substr($rec, 14*3, 2);

# Get and convert PIN
print "Enter PIN code: ";
ReadMode 'noecho';
$pin = ReadLine 0;
ReadMode 'normal';
print "\n";
chomp $pin;
$pin = pad_pin($pin) or die "pad pin\n";

# Verif CHV
# sw = 9000 if PIN Ok
# sw = 9808 if PIN disabled
($sw, $data) = $hCard->TransmitWithCheck("A0 20 00 01 08 ".$pin, "(90 00)|(98 08)", $opt_D);
die "Verify CHV: $Chipcard::PCSC::Card::Error" unless defined $sw;

for $i (1..255)
{
	# Read Record
	($sw, $rec) = $hCard->TransmitWithCheck("A0 B2 ".(uc sprintf "%02x ", $i)."04 ".$rec_size, "90 00", $opt_D);
	die "Read Record: $Chipcard::PCSC::Card::Error" unless defined $sw;

	$rec = SIM_phonebook_record($rec, hex $rec_size);
	printf "%02d: %s\n", $i, $rec;
}

exit;

sub END
{
	ReadMode 'normal';

	###############   Disconnect card   ################
	$hCard->Disconnect($Chipcard::PCSC::SCARD_LEAVE_CARD) if defined $hCard;
	undef $hCard;
	undef $hContext;
}

####################################
#
# print the Hexa and ASCII versions
#
####################################
sub SIM_phonebook_record($$)
{
	# 4C 75 64 6F FF FF FF FF FF FF FF FF FF FF 06 A8 10 32 54 76 98 FF
	# FF FF FF FF FF FF
	# Ludo : 0123456789

	# 4C 75 64 6F FF FF FF FF FF FF FF FF FF FF : name
	# 06 : number of bytes for the phone number
	# A8 : TON (Type Of Number) and NPI (Numbering Plan Identification)
	# 10 32 54 76 98 FF FF FF FF FF : phone number (reversed nibbles)
	# FF : Capability/Configuration Identifier
	# FF : extension1 (<> FF for numbers of more than 10 bytes (20 digits)

	my $data = shift;
	my $rec_size = shift;
	my ($tmp, $i, $c, @string, $rec, $delta);

	$rec = "";
	$tmp = $data;
	print "$tmp\n" if ($opt_d);

	@string = split / /, $tmp;

	# debug only
	if ($opt_d)
	{
		for $i (@string)
		{
			$c = hex $i;
			if ($c < 32)
			{
				print ".";
			}
			else
			{
				print chr $c;
			}
		}
		print "\n";
	}

	# void record
	return ("") if (hex $string[0] == 255);

	# normal size : 28 (1C) => delta = 0
	# extra size : 34 (22) => delta = 6
	$delta = $rec_size-28;

	# 14+$delta characters for the name
	# $rec_size vaut 28 (1C) ou 34 (22)
	for $i (0..(14+$delta-1))
	{
		$c = hex $string[$i];

		# FF are used to padd
		#next if ($c == 255);

		if ($c < 32)
		{
			$rec .= ".";
		}
		else
		{
			if ($c == 255)
			{
				$rec .= " ";
			}
			else
			{
				$rec .= chr $c;
			}
		}
	}

	# separator
	$rec .= " : ";
	#print " : ";

	# 14 characters for phone number
	for $i ((16+$delta)..($rec_size-1))
	{
		my ($q1, $q2);

		$q1 = substr $string[$i], 0, 1;
		$q2 = substr $string[$i], 1, 1;

		next if ($q2 eq "F");

		$rec .= $q2;
		#print $q2;

		next if ($q1 eq "F");

		$rec .= $q1;
		#print $q1;
	}

	return $rec;
}

####################################
#
# Convert and pad the pin with FF
#
####################################
sub pad_pin($)
{
	my $pin = shift;
	my ($res, $i, @a, @b);

	if (length $pin > 8)
	{
		print "PIN too long (" . (length $pin) . "), max 8\n";
		return 0;
	}
	
	$res = $pin;

	# insert a space after each char
	$res =~ s/(.)/$1 /g;

	# generate a list
	@a = split / /, $res;

	# get the ASCII code
	@a = map (ord, @a);

	# get the HEX value
	@a = map ({uc sprintf "%02x", $_} @a);

	# concatenate
	$res = join '', @a;

	# pad with FF up to 16 chars
	$res = $res . "F" x (16 - length($res));

	# insert a space after each two chars
	$res =~ s/(..)/$1 /g;

	return $res;
}

