import React from 'react'
import { Text } from 'react-localize'
import { DropdownButton, MenuItem } from 'react-bootstrap'
import { PropTypes as PM } from 'mobx-react'
import P from 'prop-types'
import { Account } from 'Utils/propTypes'
import { getJid } from 'Utils'

const AccountsProps = {
  activeAccount: Account.isRequired,
  accounts: PM.observableArrayOf(Account).isRequired,
  onChangeActiveAccount: P.func.isRequired
}

const Accounts = ({
  activeAccount,
  accounts,
  onChangeActiveAccount
}) => (
  <div id='active-account'>
    { (typeof activeAccount === 'object' && activeAccount.username && activeAccount.domain)
      ? `${activeAccount.username}@${activeAccount.domain}`
      : <Text message='message.noAccount' />
    }
    <DropdownButton
      id='accounts'
      onSelect={onChangeActiveAccount}
      bsStyle={'primary'}
      title='Accounts'
    >
      {accounts.map(jid => (
        <MenuItem
          key={getJid(jid)}
          eventKey={getJid(jid)}
          id={jid.username}
        >
          {getJid(jid)}
        </MenuItem>
      ))}
    </DropdownButton>
  </div>
)

Accounts.propTypes = AccountsProps

export default Accounts
